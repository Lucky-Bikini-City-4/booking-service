package com.dayaeyak.booking.domain.booking;

import com.dayaeyak.booking.domain.booking.dto.request.BookingCreateRequestDto;
import com.dayaeyak.booking.domain.booking.dto.request.BookingFindByServiceDto;
import com.dayaeyak.booking.domain.booking.dto.request.BookingPerformanceRequestDto;
import com.dayaeyak.booking.domain.booking.dto.request.BookingUpdateRequestDto;
import com.dayaeyak.booking.domain.booking.dto.response.BookingCreateResponseDto;
import com.dayaeyak.booking.domain.booking.dto.response.BookingFindResponseDto;
import com.dayaeyak.booking.domain.booking.enums.BookingStatus;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate; // KafkaTemplate 임포트
import org.springframework.stereotype.Service;
import com.dayaeyak.booking.domain.detail.BookingDetailRepository; // BookingDetailRepository 임포트

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate; // KafkaTemplate 주입
    private final BookingDetailRepository bookingDetailRepository; // BookingDetailRepository 주입



    private Booking findBooking(Long bookingId){
        return bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("예약을 찾을수 없습니다: " + bookingId));
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
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));
        booking.setStatus(status);
        bookingRepository.save(booking);
    }

    @Transactional
    public void confirmBooking(long bookingId) {
        Booking booking = findBooking(bookingId);
        booking.setStatus(BookingStatus.COMPLETED);
    }
}