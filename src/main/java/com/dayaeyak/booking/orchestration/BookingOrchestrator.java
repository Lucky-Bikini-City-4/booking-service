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
import com.dayaeyak.booking.client.test.PaymentClasdasdsaient;
import com.dayaeyak.booking.client.test.PerformanceClient2;
import com.dayaeyak.booking.domain.booking.Booking;
import com.dayaeyak.booking.domain.booking.BookingService;
import com.dayaeyak.booking.domain.booking.dto.request.*;
import com.dayaeyak.booking.domain.booking.dto.response.BookingCreateResponseDto;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
            default -> throw new IllegalArgumentException("Unknown service type: " + requestDto.serviceType());
        };
    }

    /**
     * 전체 오케스트레이션 진입점
     *
     * 1) Redis에 seat 키들을 선점(holdToken 사용)
     * 2) (선점 성공 시) DB에 PENDING 상태의 Booking 생성
     * 3) Redis value를 bookingId 로 덮어쓰기 (추적성 확보)
     * 4) 공연 서비스에 좌석 재확인 (DB 기준)
     * 5) 결제 요청
     * 6) 공연 서비스에 좌석 확정(판매 처리)
     * 7) Booking 상태 CONFIRMED, Redis 키 제거, 알림 발송
     */

    public BookingCreateResponseDto orchestratePerformanceBooking(BookingRequestDto requestDto) {
        
        BookingPerformanceRequestDto performanceRequest = (BookingPerformanceRequestDto) requestDto.bookingDetailRequest();

        String holdToken = "HOLD:" + UUID.randomUUID();
        List<String> lockedKeys = tryLockSeats
                (performanceRequest.sessionId(),
                        requestDto.serviceId(),
                        performanceRequest.seatNumber(),
                        holdToken);

        try {

            // 1) 공연 서비스에 좌석 유효성/동시성 재확인 (DB 기준)
//            boolean seatsAvailable = performanceClient2.checkSeatsAvailable(serviceId, seatNumbers);
//            if (!seatsAvailable) {
//                // 공연 DB 기준으로 이미 판매된 좌석이 있음 -> 보상 처리
//                throw new IllegalStateException("공연 DB상 좌석이 이미 판매되어 예약 불가합니다.");
//            }
            for(Integer seatNumber : performanceRequest.seatNumber()) {
                ApiResponse<SeatResponseDto> seatResponse = performanceClient.readPerformanceSeat(
                        performanceRequest.performanceId(),
                        performanceRequest.sessionId(),
                        performanceRequest.sectionId(),
                        performanceRequest.seatId());
                SeatResponseDto seatResponseDto = seatResponse.getData();
                if (Boolean.TRUE.equals(seatResponseDto.isSoldOut())) {
                    throw new IllegalStateException("이미 판매된 좌석입니다. seatNumber=" + seatNumber);
                }
            }

            // 2) Redis에 선점 시도 (holdToken 사용)
            if (lockedKeys.size() != performanceRequest.seatNumber().size()) {
                // 부분 선점이라도 실패: 이미 선점된 좌석이 있음 -> 확보한 키들 해제 후 실패
                releaseKeys(lockedKeys);
                throw new IllegalStateException("선택한 좌석 중 이미 선점된 좌석이 있습니다.");
            }

            // 3) 예약(PENDING) 생성 — DB에 저장 (BookingService가 내부적으로 트랜잭션 처리)
            BookingCreateRequestDto createRequestDto = new BookingCreateRequestDto(
                    requestDto.userId(),
                    requestDto.serviceId(),
                    requestDto.serviceType(),
                    requestDto.totalFee(),
                    requestDto.status());
            Booking booking = bookingService.createOrchestrationBooking(createRequestDto);

            // 4) Redis key 값에 bookingId로 덮어쓰기 (추적성 향상)
            writeBookingIdToLocks(lockedKeys, booking.getId());        ;

            // 5) 결제 요청 api 호출 -> 메세지큐 전환
            //PaymentClient.PaymentResult payResult = paymentClient.requestPayment(booking.getId(), userId, totalFee);
            PaymentCreateRequestDto paymentCreateRequestDto
                    = new PaymentCreateRequestDto(booking.getId(), PaymentStatus.PENDING,requestDto.totalFee());
            ApiResponse<PaymentRequestResponseDto> response = paymentClient.requestPayment(paymentCreateRequestDto);
            if (!response.getData().paymentStatus().equals(PaymentStatus.COMPLETED)) {
                // 결제 실패 -> 보상
                //compensateOnPaymentFailure(booking, lockedKeys, payResult);
                releaseKeys(lockedKeys);
                throw new IllegalStateException("결제 실패: 결제가 실패되었습니다. ");
            }

            // 6) 공연 서비스에 좌석 최종 확정 요청 (판매 처리) api 호출 -> 메세지큐 전환
//            boolean seatMarkResult = performanceClient2.markSeatsAsSold(serviceId, seatNumbers, booking.getId());
//            if (!seatMarkResult) {
//                // 공연 서비스 좌석 반영 실패 -> 보상 (결제는 성공했을 수 있으므로 환불 시도)
//                //compensateOnSeatMarkFailure(booking, lockedKeys);
//                throw new IllegalStateException("공연 서비스 좌석 확정 실패");
//            }
            UpdateSeatSoldOutRequestDto updateSeatSoldOutRequestDto = new UpdateSeatSoldOutRequestDto(true);
            ApiResponse<SeatResponseDto> seatResponse = performanceClient
                    .changeIsSoldOut(performanceRequest.performanceId(),
                            performanceRequest.sessionId(),
                            performanceRequest.sectionId(),
                            performanceRequest.seatId(),
                            updateSeatSoldOutRequestDto);

            if (seatResponse.getData().isSoldOut() != true){
                throw new IllegalStateException("변경에 실패했습니다.");
            }

            // 7) Booking 확정 및 Redis 정리
            log.info("예약 아이디 : " +booking.getId());
            bookingService.confirmBooking(booking.getId());
            booking = bookingService.getBookingById(booking.getId()); // DB에서 최신 상태로 다시 로드
            releaseKeys(lockedKeys);

            // 8) 디테일 저장
            List<BookingDetailPayload> payloadList = new ArrayList<>();
            LocalDateTime sessionDateTime = performanceRequest.sessionDate().atTime(performanceRequest.sessionTime());
            for(Integer seatNumber : performanceRequest.seatNumber()) {
                PerformanceBookingDetail payload =
                        new PerformanceBookingDetail(ServiceType.PERFORMANCE,
                                performanceRequest.sectionName(),
                                seatNumber,
                                sessionDateTime,
                                performanceRequest.seatPrice());
                payloadList.add(payload);
            }
            bookingDetailService.createBookingDetail(booking.getId(),payloadList);




            // 알림 발송(비동기 실패는 전체 실패로 연결하지 않음)
//            try {
//                notificationClient.sendBookingConfirmed(userId, booking.getId());
//            } catch (Exception e) {
//                log.warn("알림 전송 실패(무시): bookingId={}, err={}", booking.getId(), e.getMessage());
//            }

            // 최신 Booking 반환
            return BookingCreateResponseDto.from(booking);

        } catch (RuntimeException re) {
            // 예외는 위에서 보상 처리가 이미 됐을 수 있음.
            log.error("오케스트레이션 실패: userId={}, serviceId={}, seats={}, err={}",
                    requestDto.userId(), requestDto.serviceId(), performanceRequest.seatNumber(), re.getMessage());
            throw re;
        } catch (Exception ex) {
            // 예기치 못한 예외: 보상시도 후 예외 재던짐
            log.error("오케스트레이션에서 예기치 못한 오류: {}", ex.getMessage(), ex);
            releaseKeys(lockedKeys);
            throw new RuntimeException("예약 처리 중 오류가 발생했습니다.");
        }
    }


    public BookingCreateResponseDto orchestrateExhibitionBooking(BookingRequestDto requestDto) {

        // 1) booking 생성
        BookingCreateRequestDto createRequestDto = new BookingCreateRequestDto(
                requestDto.userId()
                ,requestDto.serviceId()
                ,requestDto.serviceType()
                ,requestDto.totalFee()
                ,requestDto.status());
        Booking booking = bookingService.createOrchestrationBooking(createRequestDto);


        // 2) 해당 정보를 가지고 결제
        PaymentCreateRequestDto paymentCreateRequestDto
                = new PaymentCreateRequestDto(booking.getId(), PaymentStatus.PENDING,requestDto.totalFee());
        ApiResponse<PaymentRequestResponseDto> response = paymentClient.requestPayment(paymentCreateRequestDto);
        if (!response.getData().paymentStatus().equals(PaymentStatus.COMPLETED)) {
            // 결제 실패 -> 보상
            //compensateOnPaymentFailure(booking, lockedKeys, payResult);
            throw new IllegalStateException("결제 실패: 결제가 실패되었습니다. ");
        }
        // 3) 디테일 생성

        List<BookingDetailPayload> payloadList = new ArrayList<>();
        BookingExhibitionRequestDto exhibitionRequest = (BookingExhibitionRequestDto) requestDto.bookingDetailRequest();
        ExhibitionBookingDetail payload =
                new ExhibitionBookingDetail(ServiceType.EXHIBITION,exhibitionRequest.grade(), exhibitionRequest.price());
        payloadList.add(payload);

        bookingDetailService.createBookingDetail(booking.getId(),payloadList);

        // 4) 업데이트
        bookingService.confirmBooking(booking.getId());
        booking = bookingService.getBookingById(booking.getId());

        return BookingCreateResponseDto.from(booking);
    }

    public BookingCreateResponseDto orchestrateRestaurantBooking(BookingRequestDto requestDto) {
        // 1) 여유 좌석 갯수 확인
        BookingRestaurantRequestDto restaurantRequest = (BookingRestaurantRequestDto) requestDto.bookingDetailRequest();
        int customerCount = restaurantRequest.customer();

        // 음식점 api 호출
        ApiResponse<SeatAvailabilityDto> seatAvailabilityDto
                = restaurantClient.getSeats(restaurantRequest.restaurantId(), restaurantRequest.date());
        if (seatAvailabilityDto.getData().getAvailableSeats() < customerCount) {
            throw new IllegalStateException("예약할 좌석이 부족합니다.");
        }

        // 1-2) 예약중 생성
        BookingCreateRequestDto createRequestDto = new BookingCreateRequestDto(
                requestDto.userId()
                , requestDto.serviceId()
                , requestDto.serviceType()
                , requestDto.totalFee()
                , requestDto.status());
        Booking booking = bookingService.createOrchestrationBooking(createRequestDto);

        // 2) 결제
        PaymentCreateRequestDto paymentCreateRequestDto
                = new PaymentCreateRequestDto(booking.getId(), PaymentStatus.PENDING, requestDto.totalFee());
        ApiResponse<PaymentRequestResponseDto> response = paymentClient.requestPayment(paymentCreateRequestDto);
        if (!response.getData().paymentStatus().equals(PaymentStatus.COMPLETED)) {
            // 결제 실패 -> 보상
            //compensateOnPaymentFailure(booking, lockedKeys, payResult);
            throw new IllegalStateException("결제 실패: 결제가 실패되었습니다. ");
        }

        // 3) 디테일 생성
        List<BookingDetailPayload> payloadList = new ArrayList<>();
        RestaurantBookingDetail payload =
                new RestaurantBookingDetail(
                        ServiceType.RESTAURANT,
                        restaurantRequest.date(),
                        restaurantRequest.time(),
                        restaurantRequest.customer());
        payloadList.add(payload);
        bookingDetailService.createBookingDetail(booking.getId(),payloadList);


        // 4) 정보 업데이트
        SeatsRequestDto seatsRequestDto = new SeatsRequestDto();
        seatsRequestDto.setRestaurantId(restaurantRequest.restaurantId());
        seatsRequestDto.setDate(LocalDate.now());
        seatsRequestDto.setCount(customerCount);
        restaurantClient.reserveSeats(seatsRequestDto);

        bookingService.confirmBooking(booking.getId());
        booking = bookingService.getBookingById(booking.getId());


        return BookingCreateResponseDto.from(booking);
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

        // -------------------------
        // Compensation handlers
        // -------------------------

        private void compensateOnPaymentFailure (Booking
        booking, List < String > lockedKeys, PaymentClasdasdsaient.PaymentResult payResult){
            // 예약 취소
            if (booking != null) {
                try {
                    //bookingService.cancelBooking(booking.getId());
                } catch (Exception e) {
                    log.error("예약 취소 실패 during payment compensation, bookingId={}, err={}", booking.getId(), e.getMessage());
                }
            }

            // Redis 해제
            releaseKeys(lockedKeys);

            // 부분 결제 등 capture가 발생했으면 환불 시도
            if (payResult != null && payResult.isPartialCaptured()) {
                try {
                    //paymentClient.refund(booking.getId());
                } catch (Exception e) {
                    log.error("refund failed for bookingId={}, err={}", booking.getId(), e.getMessage());
                }
            }
        }

        private void compensateOnSeatMarkFailure (Booking booking, List < String > lockedKeys){
            // 좌석 확정 실패: 결제 성공 상태라면 환불 시도
            if (booking != null) {
                try {
                    //paymentClient.refund(booking.getId());
                } catch (Exception e) {
                    log.error("refund failed during seatMark compensation, bookingId={}, err={}", booking.getId(), e.getMessage());
                }
                try {
                    //bookingService.cancelBooking(booking.getId());
                } catch (Exception e) {
                    log.error("booking cancel failed during seatMark compensation, bookingId={}, err={}", booking.getId(), e.getMessage());
                }
            }
            releaseKeys(lockedKeys);
        }

    }