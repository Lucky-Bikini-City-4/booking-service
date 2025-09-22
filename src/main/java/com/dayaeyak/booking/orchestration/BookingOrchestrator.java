package com.dayaeyak.booking.orchestration;

import com.dayaeyak.booking.client.payment.PaymentClient;
import com.dayaeyak.booking.client.payment.PaymentCreateRequestDto;
import com.dayaeyak.booking.client.payment.PaymentRequestResponseDto;
import com.dayaeyak.booking.client.payment.PaymentStatus;
import com.dayaeyak.booking.client.performance.PerformanceClient;
import com.dayaeyak.booking.client.performance.SeatResponseDto;
import com.dayaeyak.booking.client.performance.UpdateSeatSoldOutRequestDto;
import com.dayaeyak.booking.client.restaurant.RestaurantClient;
import com.dayaeyak.booking.client.restaurant.SeatAvailabilityDto;
import com.dayaeyak.booking.client.restaurant.SeatsRequestDto;
import com.dayaeyak.booking.client.test.PerformanceClient2;
import com.dayaeyak.booking.common.exception.CustomException;
import com.dayaeyak.booking.common.exception.ErrorCode;
import com.dayaeyak.booking.domain.booking.Booking;
import com.dayaeyak.booking.domain.booking.BookingService;
import com.dayaeyak.booking.domain.booking.dto.request.*;
import com.dayaeyak.booking.domain.booking.dto.response.BookingCreateResponseDto;
import com.dayaeyak.booking.domain.booking.enums.BookingStatus;
import com.dayaeyak.booking.domain.booking.enums.ServiceType;
import com.dayaeyak.booking.domain.detail.BookingDetailService;
import com.dayaeyak.booking.domain.detail.payload.BookingDetailPayload;
import com.dayaeyak.booking.domain.detail.payload.ExhibitionBookingDetail;
import com.dayaeyak.booking.domain.detail.payload.PerformanceBookingDetail;
import com.dayaeyak.booking.domain.detail.payload.RestaurantBookingDetail;
import com.dayaeyak.booking.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * BookingOrchestrator — 좌석 선점(Redis) 우선 → 예약(PENDING) 생성 → 결제 → 확정 흐름
 *
 * 주의사항:
 * - 외부 API 호출(결제/공연)은 네트워크 지연 발생 가능하므로 오케스트레이터 전체를 트랜잭션으로 묶지 않습니다.
 * - Redis 선점은 setIfAbsent (SETNX) + TTL 방식으로 처리합니다.
 * - 실패 시 보상(compensate): 예약 취소, Redis 선점 해제, 필요시 결제 환불 호출.
 *
 * TODO:
 * - PerformanceClient, PaymentClient, NotificationClient 를 실제 Feign/Rest 구현체로 프로젝트에 추가하세요.
 * - 필요하면 Lua 스크립트로 multi-key 원자성 보강 (대량 동시성 환경에서 권장).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingOrchestrator {

    private final BookingService bookingService;
    private final PerformanceClient2 performanceClient2;
    private final PerformanceClient performanceClient;
    private final PaymentClient paymentClient;
    private final RestaurantClient restaurantClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final BookingDetailService bookingDetailService;

    // 보유 시간: 예시 5분 (초 단위)
    private static final long HOLD_TTL_SECONDS = 5 * 60L;
    private static final String SEAT_KEY_FORMAT = "seat:%d:%d:%d"; // seat:{sessionId}:{serviceId}:{seatNumber}

    public BookingCreateResponseDto orchestrateBooking (BookingRequestDto requestDto){
        return switch (requestDto.serviceType()) {
            case PERFORMANCE -> orchestratePerformanceBooking(requestDto);
            case EXHIBITION -> orchestrateExhibitionBooking(requestDto);
            case RESTAURANT -> orchestrateRestaurantBooking(requestDto);
            default -> throw new CustomException(ErrorCode.INVALID_TYPE_VALUE);
        };
    }

    //  공연 서비스 예약 메서드
    public BookingCreateResponseDto orchestratePerformanceBooking(BookingRequestDto requestDto) {

        BookingPerformanceRequestDto performanceRequest = (BookingPerformanceRequestDto) requestDto.bookingDetailRequest();

        String holdToken = "HOLD:" + UUID.randomUUID();
        List<String> lockedKeys = tryLockSeats(
                performanceRequest.sessionId(),
                requestDto.serviceId(),
                performanceRequest.seatNumber(),
                holdToken);

        Booking booking = null; // booking 객체를 try 블록 외부에서 선언
        Long paymentId = null;


        try {
            // 1) Redis에 선점 시도 (holdToken 사용)
            if (lockedKeys.size() != performanceRequest.seatNumber().size()) {
                throw new CustomException(ErrorCode.SEAT_ALREADY_LOCKED);
            }

            // 2) 공연 서비스에 좌석 유효성/동시성 재확인 (DB 기준)
            for (Integer seatNumber : performanceRequest.seatNumber()) {
                ApiResponse<SeatResponseDto> seatResponse = performanceClient.readPerformanceSeat(
                        performanceRequest.performanceId(),
                        performanceRequest.sessionId(),
                        performanceRequest.sectionId(),
                        performanceRequest.seatId());
                SeatResponseDto seatResponseDto = seatResponse.getData();
                if (Boolean.TRUE.equals(seatResponseDto.isSoldOut())) {
                    throw new CustomException(ErrorCode.SEAT_ALREADY_SOLD);
                }
            }

            // 3) 예약(PENDING) 생성
            BookingCreateRequestDto createRequestDto = new BookingCreateRequestDto(
                    requestDto.userId(),
                    requestDto.serviceId(),
                    requestDto.serviceType(),
                    requestDto.totalFee(),
                    requestDto.status());
            booking = bookingService.createOrchestrationBooking(createRequestDto);

            // 4) Redis key 값에 bookingId로 덮어쓰기
            writeBookingIdToLocks(lockedKeys, booking.getId());

            // 5) 결제 요청
            PaymentCreateRequestDto paymentCreateRequestDto = new PaymentCreateRequestDto(booking.getId(), PaymentStatus.PENDING, requestDto.totalFee());
            ApiResponse<PaymentRequestResponseDto> response = paymentClient.requestPayment(paymentCreateRequestDto);
            if (!response.getData().paymentStatus().equals(PaymentStatus.COMPLETED)) {
                throw new CustomException(ErrorCode.PAYMENT_FAILED);
            }
            paymentId = response.getData().paymentId(); // 결제 ID 저장


            // 6) 공연 서비스에 좌석 최종 확정 요청
            UpdateSeatSoldOutRequestDto updateSeatSoldOutRequestDto = new UpdateSeatSoldOutRequestDto(true);
            ApiResponse<SeatResponseDto> seatResponse = performanceClient.changeIsSoldOut(
                    performanceRequest.performanceId(),
                    performanceRequest.sessionId(),
                    performanceRequest.sectionId(),
                    performanceRequest.seatId(),
                    updateSeatSoldOutRequestDto);

            if (seatResponse.getData().isSoldOut() != true) {
                // 이 단계의 실패는 심각한 데이터 불일치를 의미. 결제 환불 및 수동 개입 필요.
                // TODO: 결제 환불 로직 호출
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR); // 좌석 상태 변경 실패
            }

            // 7) Booking 확정 및 Redis 정리
            bookingService.confirmBooking(booking.getId());
            booking = bookingService.getBookingById(booking.getId());
            releaseKeys(lockedKeys);

            // 8) 디테일 저장
            List<BookingDetailPayload> payloadList = new ArrayList<>();
            LocalDateTime sessionDateTime = performanceRequest.sessionDate().atTime(performanceRequest.sessionTime());
            for (Integer seatNumber : performanceRequest.seatNumber()) {
                PerformanceBookingDetail payload = new PerformanceBookingDetail(ServiceType.PERFORMANCE,
                        performanceRequest.sectionName(),
                        seatNumber,
                        sessionDateTime,
                        performanceRequest.seatPrice());
                payloadList.add(payload);
            }
            bookingDetailService.createBookingDetail(booking.getId(), payloadList);

            return BookingCreateResponseDto.from(booking);

        } catch (Exception ex) {
            // 오케스트레이션 과정에서 발생한 모든 예외 처리
            log.error("공연 예약 실패: userId={}, serviceId={}, err={}",
                    requestDto.userId(), requestDto.serviceId(), ex.getMessage(), ex);

            // --- 보상 트랜잭션 시작 ---
            releaseKeys(lockedKeys); // 1. Redis 선점 해제

            if (booking != null) { // 2. Booking이 생성되었다면 CANCELED로 상태 변경
                bookingService.updateBookingStatus(booking.getId(), BookingStatus.CANCELLED);
            }
            
            if (ex instanceof CustomException && ((CustomException) ex).getErrorCode() == ErrorCode.INTERNAL_SERVER_ERROR) {
                // TODO: 좌석 확정 실패 시 결제 환불 로직을 여기에 추가해야 함
                log.error("결제 환불이 필요한 심각한 오류 발생! bookingId: {}", booking != null ? booking.getId() : "N/A");
            }
            // --- 보상 트랜잭션 종료 ---

            // GlobalExceptionHandler가 처리하도록 발생한 예외를 그대로 다시 던짐
            if (ex instanceof CustomException) {
                throw ex;
            } else {
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, ex.getMessage());
            }
        }
    }

    public BookingCreateResponseDto orchestrateExhibitionBooking(BookingRequestDto requestDto) {

        // 1. booking 생성
        BookingCreateRequestDto createRequestDto = new BookingCreateRequestDto(
                requestDto.userId(),
                requestDto.serviceId(),
                requestDto.serviceType(),
                requestDto.totalFee(),
                requestDto.status());
        Booking booking = bookingService.createOrchestrationBooking(createRequestDto);

        Long paymentId = null; // 보상 로직을 위해 외부에 선언

        try {
            // 2. 해당 정보를 가지고 결제
            PaymentCreateRequestDto paymentCreateRequestDto = new PaymentCreateRequestDto(booking.getId(), PaymentStatus.PENDING, requestDto.totalFee());
            ApiResponse<PaymentRequestResponseDto> response = paymentClient.requestPayment(paymentCreateRequestDto);
            
            // 결제 실패 시 CustomException 발생
            if (!response.getData().paymentStatus().equals(PaymentStatus.COMPLETED)) {
                throw new CustomException(ErrorCode.PAYMENT_FAILED);
            }
            paymentId = response.getData().paymentId(); // 결제 ID 저장

            // 3. 디테일 생성
            List<BookingDetailPayload> payloadList = new ArrayList<>();
            BookingExhibitionRequestDto exhibitionRequest = (BookingExhibitionRequestDto) requestDto.bookingDetailRequest();
            ExhibitionBookingDetail payload = new ExhibitionBookingDetail(ServiceType.EXHIBITION, exhibitionRequest.grade(), exhibitionRequest.price());
            payloadList.add(payload);
            bookingDetailService.createBookingDetail(booking.getId(), payloadList);

            // 4. Booking 확정
            bookingService.confirmBooking(booking.getId()); // confirm 메서드에 paymentId도 넘겨주면 좋음
            
            // 최종 상태의 booking 객체를 다시 조회
            Booking confirmedBooking = bookingService.getBookingById(booking.getId());
            return BookingCreateResponseDto.from(confirmedBooking);

        } catch (Exception ex) {
            log.error("Exhibition booking failed for bookingId: {}. Error: {}", booking.getId(), ex.getMessage(), ex);

            // [보상 트랜잭션]
            // 결제가 생성되었다면(paymentId가 있다면), 결제 취소 요청
            if (paymentId != null) {
                log.warn("Compensation: Attempting to cancel payment. paymentId: {}", paymentId);
                // 환불 기능 생성: 예약 서비스 paymentClient.cancelPayment(paymentId);
            }

            // Booking 상태를 CANCELED로 변경
            bookingService.updateBookingStatus(booking.getId(), BookingStatus.CANCELLED);

            // 일관된 예외 처리를 위해 다시 던짐
            if (ex instanceof CustomException) {
                throw ex;
            }
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    public BookingCreateResponseDto orchestrateRestaurantBooking(BookingRequestDto requestDto) {
        // 1) 여유 좌석 갯수 확인
        BookingRestaurantRequestDto restaurantRequest = (BookingRestaurantRequestDto) requestDto.bookingDetailRequest();
        int customerCount = restaurantRequest.customer();

        // 음식점 api 호출
        ApiResponse<SeatAvailabilityDto> seatAvailabilityDto
                = restaurantClient.getSeats(restaurantRequest.restaurantId(), restaurantRequest.date());
        if (seatAvailabilityDto.getData().getAvailableSeats() < customerCount) {
            throw new CustomException(ErrorCode.SEAT_NOT_AVAILABLE);
        }

        // 2) 예약중 생성
        BookingCreateRequestDto createRequestDto = new BookingCreateRequestDto(
                requestDto.userId()
                , requestDto.serviceId()
                , requestDto.serviceType()
                , requestDto.totalFee()
                , requestDto.status());
        Booking booking = bookingService.createOrchestrationBooking(createRequestDto);
        Long paymentId = null;

        try {
            // 3) 결제
            PaymentCreateRequestDto paymentCreateRequestDto
                    = new PaymentCreateRequestDto(booking.getId(), PaymentStatus.PENDING, requestDto.totalFee());
            ApiResponse<PaymentRequestResponseDto> response = paymentClient.requestPayment(paymentCreateRequestDto);
            if (!response.getData().paymentStatus().equals(PaymentStatus.COMPLETED)) {
                throw new CustomException(ErrorCode.PAYMENT_FAILED);
            }

            paymentId = response.getData().paymentId();

            // 4) 레스토랑 서비스에 좌석 예약 확정
            SeatsRequestDto seatsRequestDto = new SeatsRequestDto();
            seatsRequestDto.setRestaurantId(restaurantRequest.restaurantId());
            seatsRequestDto.setDate(restaurantRequest.date());
            seatsRequestDto.setCount(customerCount);
            restaurantClient.reserveSeats(seatsRequestDto);

            // 5) 디테일 생성
            List<BookingDetailPayload> payloadList = new ArrayList<>();
            RestaurantBookingDetail payload =
                    new RestaurantBookingDetail(
                            ServiceType.RESTAURANT,
                            restaurantRequest.date(),
                            restaurantRequest.time(),
                            restaurantRequest.customer());
            payloadList.add(payload);
            bookingDetailService.createBookingDetail(booking.getId(),payloadList);

            bookingService.confirmBooking(booking.getId());
            booking = bookingService.getBookingById(booking.getId());


            return BookingCreateResponseDto.from(booking);

        }
        catch (Exception ex) {
            log.error("레스토랑 예약 실패: bookingId={}, userId={}, error={}",
                    booking.getId(), requestDto.userId(), ex.getMessage(), ex);

            if (paymentId != null) {
                log.warn("보상: 결제 취소 시도. paymentId: {}", paymentId);
                // TODO: paymentClient.cancelPayment(paymentId) 구현 필요
            }

            bookingService.updateBookingStatus(booking.getId(), BookingStatus.CANCELLED);

            if (ex instanceof CustomException) {
                throw ex;
            }
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, ex.getMessage());

        }
    }




        // -------------------------
        // Redis lock helpers
        // -------------------------

        /**
         * 여러 좌석을 setIfAbsent 로 선점 시도.
         * 성공한 키 리스트를 반환 (실패 시 이미 성공한 키는 호출자에서 release 처리)
         */
        private List<String> tryLockSeats (Long sessionId, Long serviceId, List < Integer > seatNumbers, String
        holdToken){
            List<String> lockedKeys = new ArrayList<>();
            for (Integer seat : seatNumbers) {
                String key = String.format(SEAT_KEY_FORMAT, sessionId, serviceId, seat);
                Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, holdToken, HOLD_TTL_SECONDS, TimeUnit.SECONDS);
                if (Boolean.TRUE.equals(ok)) {
                    lockedKeys.add(key);
                } else {
                    // 실패하면 즉시 반환(호출자에서 releaseKeys 처리)
                    log.debug("Redis lock failed for key={}", key);
                    break;
                }
            }
            return lockedKeys;
        }

        private void writeBookingIdToLocks (List < String > keys, Long bookingId){
            String bookingIdStr = String.valueOf(bookingId);
            for (String key : keys) {
                try {
                    // overwrite value with bookingId and reset TTL
                    redisTemplate.opsForValue().set(key, bookingIdStr, HOLD_TTL_SECONDS, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.warn("Redis write bookingId failed for key={}, bookingId={}, err={}", key, bookingId, e.getMessage());
                }
            }
        }

        private void releaseKeys (List < String > keys) {
            if (keys == null || keys.isEmpty()) return;
            try {
                redisTemplate.delete(keys);
            } catch (Exception e) {
                log.warn("Redis delete keys failed: {}, err={}", keys, e.getMessage());
            }
        }
    }