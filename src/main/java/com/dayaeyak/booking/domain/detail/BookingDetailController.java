package com.dayaeyak.booking.domain.detail;

import com.dayaeyak.booking.common.exception.CustomException;
import com.dayaeyak.booking.common.exception.ErrorCode;
import com.dayaeyak.booking.domain.booking.Booking;
import com.dayaeyak.booking.domain.booking.BookingService;
import com.dayaeyak.booking.domain.booking.enums.ServiceType;
import com.dayaeyak.booking.domain.detail.dto.request.BookingDetailCreateRequestDto;
import com.dayaeyak.booking.domain.detail.dto.request.BookingDetailUpdateRequestDto;
import com.dayaeyak.booking.domain.detail.dto.response.BookingDetailCreateResponseDto;
import com.dayaeyak.booking.domain.detail.dto.response.BookingDetailFindResponseDto;
import com.dayaeyak.booking.domain.detail.payload.BookingDetailPayload;
import com.dayaeyak.booking.domain.detail.payload.ExhibitionBookingDetail;
import com.dayaeyak.booking.domain.detail.payload.PerformanceBookingDetail;
import com.dayaeyak.booking.domain.detail.payload.RestaurantBookingDetail;
import com.dayaeyak.booking.utils.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bookings/{bookingId}/details")
public class BookingDetailController {

    private final BookingDetailService bookingDetailService;
    private final BookingService bookingService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingDetailCreateResponseDto>> createBookingDetail(
            @PathVariable Long bookingId,
            @RequestBody BookingDetailCreateRequestDto requestDto) {

        if (!bookingId.equals(requestDto.bookingId())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Booking booking = bookingService.getBookingById(bookingId);

        Class<? extends BookingDetailPayload> targetPayloadClass;
        switch (booking.getServiceType()) {
            case EXHIBITION:
                targetPayloadClass = ExhibitionBookingDetail.class;
                break;
            case PERFORMANCE:
                targetPayloadClass = PerformanceBookingDetail.class;
                break;
            case RESTAURANT:
                targetPayloadClass = RestaurantBookingDetail.class;
                break;
            default:
                throw new CustomException(ErrorCode.INVALID_TYPE_VALUE);
        }

        List<BookingDetailPayload> specificPayloads = new ArrayList<BookingDetailPayload>();
        BookingDetailPayload specificPayload;
        try {
            specificPayload = objectMapper.convertValue(requestDto.details(), targetPayloadClass);
            specificPayloads.add(specificPayload);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return ApiResponse.success(HttpStatus.CREATED,
                "예약 상세 정보가 생성되었습니다",
                bookingDetailService.createBookingDetail(bookingId,specificPayloads));
    }

    @GetMapping("/{detailId}")
    public ResponseEntity<ApiResponse<BookingDetailFindResponseDto>> getBookingDetailById(
            @PathVariable Long bookingId,
            @PathVariable Long detailId) {
        return ApiResponse.success(HttpStatus.OK, bookingDetailService.findBookingDetailById(bookingId,detailId));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingDetailFindResponseDto>>> findAllBookingDetails(
            @PathVariable Long bookingId) {
        return ApiResponse.success(HttpStatus.OK, bookingDetailService.findAllBookingDetailsByBookingId(bookingId));
    }

    @PatchMapping("/{detailId}")
    public ResponseEntity<ApiResponse<Void>> updateBookingDetail(
            @PathVariable Long bookingId,
            @PathVariable Long detailId,
            @RequestBody BookingDetailUpdateRequestDto requestDto) {
        bookingDetailService.updateBookingDetail(bookingId, detailId, requestDto);
        return ApiResponse.success(HttpStatus.OK,"예약 상세 정보가 수정되었습니다.");

    }

    @DeleteMapping("/{detailId}")
    public ResponseEntity<ApiResponse<Void>> deleteBookingDetail(
            @PathVariable Long bookingId,
            @PathVariable Long detailId) {
        bookingDetailService.deleteBookingDetail(bookingId, detailId);
        return ApiResponse.success(HttpStatus.OK,"예약 상세 정보가 삭제되었습니다.");
    }



}