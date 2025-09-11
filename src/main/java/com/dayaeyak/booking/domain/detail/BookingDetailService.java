package com.dayaeyak.booking.domain.detail;

import com.dayaeyak.booking.domain.detail.dto.request.BookingDetailCreateRequestDto;
import com.dayaeyak.booking.domain.detail.dto.request.BookingDetailUpdateRequestDto;
import com.dayaeyak.booking.domain.detail.dto.response.BookingDetailCreateResponseDto;
import com.dayaeyak.booking.domain.detail.dto.response.BookingDetailFindResponseDto;
import com.dayaeyak.booking.domain.detail.payload.BookingDetailPayload;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingDetailService {

    private final BookingDetailRepository bookingDetailRepository;

    // Modified private helper method to include bookingId validation
    private BookingDetail findBookingDetail(Long bookingId, Long id) {
        BookingDetail bookingDetail = bookingDetailRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("BookingDetail을 찾을 수 없습니다: " + id));
        if (!bookingDetail.getBookingId().equals(bookingId)) {
            throw new IllegalArgumentException("BookingDetail with ID " + id + " does not belong to Booking ID " + bookingId);
        }
        return bookingDetail;
    }

    @Transactional
    public BookingDetailCreateResponseDto createBookingDetail(Long bookingId, BookingDetailPayload specificPayload) { // Modified signature
        if (!bookingDetailRepository.findByBookingId(bookingId).isEmpty()) {
            throw new IllegalArgumentException("Booking ID " + bookingId + " already has a detail.");
        }
        BookingDetail bookingDetail = new BookingDetail(); // Create new BookingDetail
        bookingDetail.setBookingId(bookingId); // Set bookingId
        bookingDetail.setDetails(specificPayload); // Set specificPayload
        bookingDetailRepository.save(bookingDetail);
        return BookingDetailCreateResponseDto.from(bookingDetail);
    }

    public BookingDetailFindResponseDto findBookingDetailById(Long bookingId, Long id) {
        BookingDetail bookingDetail = findBookingDetail(bookingId, id);
        return BookingDetailFindResponseDto.from(bookingDetail);
    }

    public List<BookingDetailFindResponseDto> findAllBookingDetailsByBookingId(Long bookingId) {
        return bookingDetailRepository.findByBookingId(bookingId)
                .stream()
                .map(BookingDetailFindResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateBookingDetail(Long bookingId, Long id, BookingDetailUpdateRequestDto requestDto) {
        BookingDetail bookingDetail = findBookingDetail(bookingId, id);
        bookingDetail.update(requestDto);
    }

    @Transactional
    public void deleteBookingDetail(Long bookingId, Long id) {
        BookingDetail bookingDetail = findBookingDetail(bookingId, id);
        bookingDetail.delete();
    }
}