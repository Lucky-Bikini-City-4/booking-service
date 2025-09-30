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
import com.dayaeyak.booking.domain.booking.BookingKafkaService;
import com.dayaeyak.booking.domain.booking.BookingService;
import com.dayaeyak.booking.domain.booking.dto.kafka.BookingRequestKafkaDto;
import com.dayaeyak.booking.domain.booking.dto.kafka.SeatStatusDto;
import com.dayaeyak.booking.domain.booking.dto.kafka.SeatUpdateDto;
import com.dayaeyak.booking.domain.booking.dto.request.*;
import com.dayaeyak.booking.domain.booking.dto.response.BookingCreateResponseDto;
import com.dayaeyak.booking.domain.booking.enums.BookingStatus;
import com.dayaeyak.booking.domain.booking.enums.ServiceType;
import com.dayaeyak.booking.domain.detail.BookingDetailService;
import com.dayaeyak.booking.domain.detail.payload.*;
import com.dayaeyak.booking.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
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
    private final BookingKafkaService bookingKafkaService;
    private final DefaultRedisScript<Long> lockSeatsScript;
    private final DefaultRedisScript<Long> confirmBooking;
    private final DefaultRedisScript<Long> releaseSeats;


    // 보유 시간: 예시 5분 (초 단위)
    private static final long HOLD_TTL_SECONDS = 5 * 60L;
    private static final long HOLD_TTL_Hours = 10L;
    private static final String SEAT_KEY_FORMAT = "seat:%d:%d:%d"; // seat:{sessionId}:{serviceId}:{seatId}
    private static final String SEAT_HASH_KEY_FORMAT = "seat:%d:%d:%d"; // serviceId, sessionId, sectionId



    public BookingCreateResponseDto orchestrateBooking (BookingRequestDto requestDto){
        return switch (requestDto.serviceType()) {
            case PERFORMANCE -> orchestratePerformanceBooking2(requestDto);
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
                requestDto.serviceId(),
                performanceRequest.sessionId(),
                performanceRequest.seatIds(),
                holdToken);

        Booking booking = null; // booking 객체를 try 블록 외부에서 선언
        Long paymentId = null;

        try {
            // 1) Redis에 선점 시도 (holdToken 사용)
            if (lockedKeys.size() != performanceRequest.seatIds().size()) {
                throw new CustomException(ErrorCode.SEAT_ALREADY_LOCKED);
            }

            // 2) 공연 서비스에 좌석 유효성/동시성 재확인 (DB 기준)
            for (Long seatId : performanceRequest.seatIds()) {
                ApiResponse<SeatResponseDto> seatResponse = performanceClient.readPerformanceSeat(
                        performanceRequest.performanceId(),
                        performanceRequest.sessionId(),
                        performanceRequest.sectionId(),
                        seatId);
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
            for (Long seatId : performanceRequest.seatIds()) {
                ApiResponse<SeatResponseDto> seatResponse = performanceClient.changeIsSoldOut(
                        performanceRequest.performanceId(),
                        performanceRequest.sessionId(),
                        performanceRequest.sectionId(),
                        seatId,
                        updateSeatSoldOutRequestDto);
            }

            // 7) Booking 확정 및 Redis 정리
            bookingService.confirmBooking(booking.getId());
            booking = bookingService.getBookingById(booking.getId());
            releaseKeys(lockedKeys);

            // 8) 디테일 저장
            List<BookingSeatRequestDto> seatRequests = BookingSeatRequestDto.from(performanceRequest);
            List<SeatInfo> seatInfos = new ArrayList<>();
            for (BookingSeatRequestDto seat : seatRequests) {
                // 각 좌석의 ID, 번호, 가격 정보를 사용하여 SeatInfo 객체를 만듭니다.
                seatInfos.add(new SeatInfo(
                        seat.seatId(),
                        seat.seatNumber(),
                        seat.seatPrice()
                ));
            }

            PerformanceBookingDetail payload = new PerformanceBookingDetail(
                    booking.getServiceId(),      // performanceId
                    performanceRequest.sessionId(),
                    performanceRequest.sectionId(),
                    // 모든 좌석이 같은 구역(section)에 있다고 가정
                    seatRequests.isEmpty() ? null : seatRequests.get(0).sectionName(),
                    performanceRequest.sessionDate().atTime(performanceRequest.sessionTime()),
                    seatInfos
            );
            bookingDetailService.createBookingDetail(booking.getId(), payload);

            // 9) 알람 전송
            String testval = String.valueOf(booking.getId());
            BookingRequestKafkaDto dto = new BookingRequestKafkaDto(1L,ServiceType.PERFORMANCE,1L,testval,"sevicename",null);

            bookingKafkaService.sendBookingRequest("1","1", dto);

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


    public BookingCreateResponseDto orchestratePerformanceBooking2(BookingRequestDto requestDto) {
        BookingPerformanceRequestDto performanceRequest =
                (BookingPerformanceRequestDto) requestDto.bookingDetailRequest();

        Booking booking = null;
        Long paymentId = null;
        boolean seatsLocked = false;
        List<Long> seatIds = performanceRequest.seatIds();

        // Redis Key 생성
        String redisKey = String.format("seat:%d:%d:%d",
                performanceRequest.performanceId(),
                performanceRequest.sessionId(),
                performanceRequest.sectionId());

        try {

            // 1) 공연 좌석 초기화 (Redis에 없으면 로딩)
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
                ApiResponse<List<SeatStatusDto>> seatList = performanceClient.readPerformanceSeats(
                        performanceRequest.performanceId(),
                        performanceRequest.sessionId(),
                        performanceRequest.sectionId()
                );

                Map<String, String> seatMap = new HashMap<>();
                for (SeatStatusDto seatStatus : seatList.getData()) { // SeatStatusDto 리스트 순회
                    String seatKey = String.valueOf(seatStatus.getSeatId()); // 실제 seatId 사용
                    seatMap.put(seatKey, seatStatus.isBooked() ? "booked" : "available");
                }
                redisTemplate.opsForHash().putAll(redisKey, seatMap);
                redisTemplate.expire(redisKey, Duration.ofHours(1)); // 캐시 TTL (옵션)
            }

            // 2) Lua Script로 좌석 선점 (locked 상태로 변경)
            String holdToken = "HOLD:" + UUID.randomUUID();

            Long result = redisTemplate.execute(
                    lockSeatsScript, // Lua Script Bean
                    Collections.singletonList(redisKey),
                    seatIds.stream().map(String::valueOf).toArray(String[]::new)
            );

            if (result == null || result == 0L) {
                throw new CustomException(ErrorCode.SEAT_ALREADY_LOCKED);
            }

            seatsLocked = true; // 선점 상태 확인 flag


            // 3) 예약(PENDING) 생성
            BookingCreateRequestDto createRequestDto = new BookingCreateRequestDto(
                    requestDto.userId(),
                    requestDto.serviceId(),
                    requestDto.serviceType(),
                    requestDto.totalFee(),
                    requestDto.status()
            );
            booking = bookingService.createOrchestrationBooking(createRequestDto);

            // 4) 결제 요청
            PaymentCreateRequestDto paymentCreateRequestDto =
                    new PaymentCreateRequestDto(booking.getId(), PaymentStatus.PENDING, requestDto.totalFee());

            ApiResponse<PaymentRequestResponseDto> response = paymentClient.requestPayment(paymentCreateRequestDto);

            if (!response.getData().paymentStatus().equals(PaymentStatus.COMPLETED)) {
                throw new CustomException(ErrorCode.PAYMENT_FAILED);
            }
            paymentId = response.getData().paymentId(); // 결제 ID 저장

            // 5) 결제 완료 → 좌석 booked 상태 반영 (Lua Script 활용 가능)
            redisTemplate.execute(
                    confirmBooking,
                    Collections.singletonList(redisKey),
                    seatIds.stream().map(String::valueOf).toArray(String[]::new)
            );

            bookingService.confirmBooking(booking.getId());

            // 6) 공연 서비스에 좌석 최종 확정 요청

            bookingKafkaService.send(requestDto);

            // 7) BookingDetail 저장
            List<BookingSeatRequestDto> seatRequests = BookingSeatRequestDto.from(performanceRequest);
            List<SeatInfo> seatInfos = new ArrayList<>();

            for (BookingSeatRequestDto seat : seatRequests) {
                // 각 좌석의 ID, 번호, 가격 정보를 사용하여 SeatInfo 객체를 만듭니다.
                seatInfos.add(new SeatInfo(
                        seat.seatId(),
                        seat.seatNumber(),
                        seat.seatPrice()
                ));
            }

            PerformanceBookingDetail payload = new PerformanceBookingDetail(
                    booking.getServiceId(),      // performanceId
                    performanceRequest.sessionId(),
                    performanceRequest.sectionId(),
                    // 모든 좌석이 같은 구역(section)에 있다고 가정
                    seatRequests.isEmpty() ? null : seatRequests.get(0).sectionName(),
                    performanceRequest.sessionDate().atTime(performanceRequest.sessionTime()),
                    seatInfos // 생성된 SeatInfo 리스트
            );
            bookingDetailService.createBookingDetail(booking.getId(), payload);

            // 8) 알람 발송 (Kafka)
            String testval = String.valueOf(booking.getId());
            BookingRequestKafkaDto dto = new BookingRequestKafkaDto(
                    1L,ServiceType.PERFORMANCE,1L,testval,"sevicename",null);

            bookingKafkaService.sendBookingRequest("booking","1", dto);

            return BookingCreateResponseDto.from(booking);

        } catch (Exception ex) {
            log.error("공연 예약 실패: userId={}, serviceId={}, err={}",
                    requestDto.userId(), requestDto.serviceId(), ex.getMessage(), ex);

            if (seatsLocked) {
                // Redis 상태 복구
                redisTemplate.execute(
                        releaseSeats,
                        Collections.singletonList(redisKey),
                        seatIds.stream().map(String::valueOf).toArray(String[]::new)
                );
            }


            if (booking != null) {
                bookingService.updateBookingStatus(booking.getId(), BookingStatus.CANCELLED);
            }

            if (paymentId != null) {
                paymentClient.refundPayment(booking.getId());
                log.error("결제 실패 환불을 진행했습니다");
            }

            // GlobalExceptionHandler가 처리하도록 발생한 예외를 그대로 다시 던짐
            if (ex instanceof CustomException) {
                throw ex;
            } else {
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
            }
        }
    }


    public BookingCreateResponseDto orchestrateExhibitionBooking(BookingRequestDto requestDto) {

        // 보상 로직을 위해 외부에 선언
        Booking booking = null;
        Long paymentId = null;

        try {
            // 1. booking 생성
            BookingCreateRequestDto createRequestDto = new BookingCreateRequestDto(
                    requestDto.userId(),
                    requestDto.serviceId(),
                    requestDto.serviceType(),
                    requestDto.totalFee(),
                    requestDto.status());
            booking = bookingService.createOrchestrationBooking(createRequestDto);

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
            ExhibitionBookingDetail payload = new ExhibitionBookingDetail(exhibitionRequest.grade(), exhibitionRequest.price());
            payloadList.add(payload);
            bookingDetailService.createBookingDetail(booking.getId(), (BookingDetailPayload) payloadList);

            // 4. Booking 확정
            bookingService.confirmBooking(booking.getId()); // confirm 메서드에 paymentId도 넘겨주면 좋음
            
            // 최종 상태의 booking 객체를 다시 조회
            Booking confirmedBooking = bookingService.getBookingById(booking.getId());
            return BookingCreateResponseDto.from(confirmedBooking);

        } catch (Exception ex) {
            log.error("Exhibition booking failed for bookingId: {}. Error: {}", booking.getId(), ex.getMessage(), ex);

            if (paymentId != null) {
                paymentClient.refundPayment(booking.getId());
                log.error("결제 실패 환불을 진행했습니다");
            }

            // Booking 상태를 CANCELED로 변경
            if (booking != null) {
                bookingService.updateBookingStatus(booking.getId(), BookingStatus.CANCELLED);
            }


            // 일관된 예외 처리를 위해 다시 던짐
            if (ex instanceof CustomException) {
                throw ex;
            }
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    public BookingCreateResponseDto orchestrateRestaurantBooking(BookingRequestDto requestDto) {
        Long paymentId = null;
        Booking booking = null;
        try {
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
                    requestDto.userId(),
                    requestDto.serviceId(),
                    requestDto.serviceType(),
                    requestDto.totalFee(),
                    requestDto.status());
            booking = bookingService.createOrchestrationBooking(createRequestDto);

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
                            restaurantRequest.date(),
                            restaurantRequest.time(),
                            restaurantRequest.customer());
            payloadList.add(payload);
            bookingDetailService.createBookingDetail(booking.getId(), (BookingDetailPayload) payloadList);

            bookingService.confirmBooking(booking.getId());
            booking = bookingService.getBookingById(booking.getId());


            return BookingCreateResponseDto.from(booking);

        }
        catch (Exception ex) {
            log.error("레스토랑 예약 실패: bookingId={}, userId={}, error={}",
                    booking.getId(), requestDto.userId(), ex.getMessage(), ex);

            if (paymentId != null) {
                paymentClient.refundPayment(booking.getId());
                log.error("결제 실패 환불을 진행했습니다");
            }

            // Booking 상태를 CANCELED로 변경
            if (booking != null) {
                bookingService.updateBookingStatus(booking.getId(), BookingStatus.CANCELLED);
            }

            if (ex instanceof CustomException) {
                throw ex;
            }

            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

        // -------------------------
        // Redis lock helpers
        // -------------------------
        private List<String> tryLockSeats(Long sessionId, Long serviceId, List<Long> seatIds, String holdToken) {
            List<String> lockedKeys = new ArrayList<>();
            for (Long seatId : seatIds) {
                String key = String.format(SEAT_KEY_FORMAT, sessionId, serviceId, seatId);
                Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, holdToken, HOLD_TTL_Hours, TimeUnit.HOURS);
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
                    redisTemplate.opsForValue().set(key, bookingIdStr, HOLD_TTL_Hours, TimeUnit.HOURS);
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