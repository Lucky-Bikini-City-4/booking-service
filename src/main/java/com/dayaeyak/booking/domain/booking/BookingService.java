package com.dayaeyak.booking.domain.booking;

import com.dayaeyak.booking.domain.booking.dto.request.BookingCreateRequestDto;
import com.dayaeyak.booking.domain.booking.dto.request.BookingFindByServiceDto;
import com.dayaeyak.booking.domain.booking.dto.request.BookingUpdateRequestDto;
import com.dayaeyak.booking.domain.booking.dto.response.BookingCreateResponseDto;
import com.dayaeyak.booking.domain.booking.dto.response.BookingFindResponseDto;
import com.dayaeyak.booking.domain.booking.enums.ServiceType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;


    private Booking findBooking(Long bookingId){
        return bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("예약을 찾을수 없습니다: " + bookingId));
    }

    @Transactional
    public BookingCreateResponseDto createBooking(BookingCreateRequestDto requestDto) {
        Booking booking = new Booking(requestDto);
        bookingRepository.save(booking);
        return BookingCreateResponseDto.from(booking);
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
}
