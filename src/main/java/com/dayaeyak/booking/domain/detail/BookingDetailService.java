package com.dayaeyak.booking.domain.detail;

import com.dayaeyak.booking.common.exception.CustomException;
import com.dayaeyak.booking.common.exception.ErrorCode;
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

    private BookingDetail findBookingDetail(Long bookingId, Long id) {
        BookingDetail bookingDetail = bookingDetailRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOKING_DETAIL_NOT_FOUND));
        if (!bookingDetail.getBookingId().equals(bookingId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return bookingDetail;
    }

    @Transactional
    public BookingDetailCreateResponseDto createBookingDetail(Long bookingId, BookingDetailPayload specificPayload) { // Modified signature
        if (!bookingDetailRepository.findByBookingId(bookingId).isEmpty()) {
            throw new CustomException(ErrorCode.DUPLICATE_BOOKING_DETAIL);
        }
        BookingDetail bookingDetail = new BookingDetail(); // Create new BookingDetail
        bookingDetail.setBookingId(bookingId); // Set bookingId
        bookingDetail.setDetails(specificPayload); // Set specificPayloads
        bookingDetailRepository.save(bookingDetail);
        return BookingDetailCreateResponseDto.from(bookingDetail);
    }

    public BookingDetailFindResponseDto findBookingDetailById(Long bookingId, Long detailId) {
        BookingDetail bookingDetail = findBookingDetail(bookingId, detailId);
        return BookingDetailFindResponseDto.from(bookingDetail);
    }

    public List<BookingDetailFindResponseDto> findAllBookingDetailsByBookingId(Long bookingId) {
        return  bookingDetailRepository
                .findByBookingId(bookingId)
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

    public List<BookingDetail> findBookingDetailByBookingId(Long bookingId) {
        return bookingDetailRepository.findByBookingId(bookingId);
    }
}