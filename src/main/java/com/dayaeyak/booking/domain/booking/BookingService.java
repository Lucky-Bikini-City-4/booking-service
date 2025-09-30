package com.dayaeyak.booking.domain.booking;

import com.dayaeyak.booking.client.payment.PaymentClient;
import com.dayaeyak.booking.common.exception.CustomException;
import com.dayaeyak.booking.common.exception.ErrorCode;
import com.dayaeyak.booking.domain.booking.dto.request.BookingCreateRequestDto;
import com.dayaeyak.booking.utils.ApiResponse;
import com.dayaeyak.booking.domain.booking.dto.request.BookingFindByServiceDto;
import com.dayaeyak.booking.domain.booking.dto.request.BookingPerformanceRequestDto;
import com.dayaeyak.booking.domain.booking.dto.request.BookingUpdateRequestDto;
import com.dayaeyak.booking.domain.booking.dto.response.BookingCreateResponseDto;
import com.dayaeyak.booking.domain.booking.dto.response.BookingFindResponseDto;
import com.dayaeyak.booking.domain.booking.enums.BookingStatus;

import com.dayaeyak.booking.domain.detail.BookingDetail;
import com.dayaeyak.booking.domain.detail.payload.PerformanceBookingDetail;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate; // KafkaTemplate 임포트
import org.springframework.stereotype.Service;
import com.dayaeyak.booking.domain.detail.BookingDetailRepository; // BookingDetailRepository 임포트

import java.util.List;
import java.util.stream.Collectors;

import com.dayaeyak.booking.domain.detail.BookingDetailService;
import org.springframework.data.redis.core.RedisTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final BookingDetailRepository bookingDetailRepository;
    private final PaymentClient paymentClient;
    private final RedisTemplate<String, String> redisTemplate; // 의존성 추가
    private final BookingDetailService bookingDetailService; // 의존성 추가


    private Booking findBooking(Long bookingId){
        return bookingRepository.findById(bookingId).orElseThrow(() -> new CustomException(ErrorCode.BOOKING_NOT_FOUND));
    }

    // 디테일 호출용
    public Booking getBookingById(Long bookingId) {
        return findBooking(bookingId);
    }

    @Transactional
    public BookingCreateResponseDto createBooking(BookingCreateRequestDto requestDto) {
        Booking booking = new Booking(requestDto);
        bookingRepository.save(booking);
        return BookingCreateResponseDto.from(booking);
    }

    @Transactional
    public Booking createOrchestrationBooking(BookingCreateRequestDto requestDto) {
        Booking booking = new Booking(requestDto);
        bookingRepository.save(booking);
        return booking;
    }

    public List<BookingFindResponseDto> findBookings() {
        return bookingRepository.findAll()
                .stream()
                .map(BookingFindResponseDto::from)
                .collect(Collectors.toList());
    }

    public BookingFindResponseDto findBookingById(Long bookingId) {
        Booking booking = findBooking(bookingId);
        return BookingFindResponseDto.from(booking);
    }

    @Transactional
    public void updateBooking(Long bookingId, BookingUpdateRequestDto requestDto) {
        Booking booking = findBooking(bookingId);
        booking.update(requestDto);
    }

    @Transactional
    public void deleteBooking(Long bookingId) {
        Booking booking = findBooking(bookingId);
        booking.delete();
    }

    public List<BookingFindResponseDto> findBookingsByService(BookingFindByServiceDto requestDto) {
        return bookingRepository.findByServiceTypeAndServiceId(requestDto.serviceType(), requestDto.serviceId())
                .stream()
                .map(BookingFindResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateBookingStatus(Long bookingId, BookingStatus status) {
        // This method would be called by the orchestrator for status updates only
        Booking booking = findBooking(bookingId);
        booking.setStatus(status);
        bookingRepository.save(booking);
    }

    @Transactional
    public void confirmBooking(long bookingId) {
        Booking booking = findBooking(bookingId);
        booking.setStatus(BookingStatus.COMPLETED);
    }

    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = findBooking(bookingId);
        booking.cancel();

        // Feign Client를 통한 환불 요청
        ApiResponse<Boolean> response = paymentClient.refundPayment(bookingId);
        if (response == null || response.getData() == null || !response.getData()) {
            throw new CustomException(ErrorCode.PAYMENT_FAILED);
        }

        // [신규] 좌석 해제 지연 작업 스케줄링
        //scheduleSeatRelease(booking);
    }

//    private void scheduleSeatRelease(Booking booking) {
//        // 이 예약과 관련된 상세 정보들을 가져옵니다.
//        List<BookingDetail> details = bookingDetailService.findBookingDetailByBookingId(booking.getId());
//
//        // Performance 예약에 대해서만 좌석 해제 로직을 실행합니다.
//        details.stream()
//                .flatMap(detail -> detail.getDetails().stream()) // flatMap으로 payload 리스트를 단일 스트림으로 펼침
//                .filter(payload -> payload instanceof PerformanceBookingDetail)
//                .map(payload -> (PerformanceBookingDetail) payload)
//                .forEach(perfDetail -> {
//                    // 1. 랜덤한 지연 시간 계산 (예: 1분 ~ 5분 사이)
//                    long minDelaySeconds = 60;
//                    long maxDelaySeconds = 300;
//                    long randomDelay = minDelaySeconds + (long) (Math.random() * (maxDelaySeconds - minDelaySeconds));
//                    long releaseTimestamp = System.currentTimeMillis() + (randomDelay * 1000);
//
//                    // 2. 작업 정보 생성 (스케줄러가 이 정보를 보고 redisKey를 다시 만들 수 있도록)
//                    // 형식: "performanceId:sessionId:sectionId:seatId"
//                    String jobValue = String.format("%d:%d:%d:%d",
//                            perfDetail.performanceId(),
//                            perfDetail.sessionId(),
//                            perfDetail.sectionId(),
//                            perfDetail.seatId());
//
//                    // 3. Redis Sorted Set에 작업 추가 (score: 실행 시간, value: 작업 정보)
//                    redisTemplate.opsForZSet().add("seat:release:queue", jobValue, releaseTimestamp);
//                    log.info("Scheduled seat release for job '{}' at timestamp {}", jobValue, releaseTimestamp);
//                });
//    }
}
