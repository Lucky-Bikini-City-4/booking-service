package com.dayaeyak.booking.domain.booking;

import com.dayaeyak.booking.domain.booking.dto.request.BookingCreateRequestDto;
import com.dayaeyak.booking.domain.booking.dto.request.BookingFindByServiceDto;
import com.dayaeyak.booking.domain.booking.dto.request.BookingUpdateRequestDto;
import com.dayaeyak.booking.domain.booking.dto.response.BookingCreateResponseDto;
import com.dayaeyak.booking.domain.booking.dto.response.BookingFindResponseDto;
import com.dayaeyak.booking.domain.booking.enums.ServiceType;
import com.dayaeyak.booking.utils.ApiResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingCreateResponseDto>> createBooking(
            @RequestBody BookingCreateRequestDto requestDto ) {

        return ApiResponse.success(HttpStatus.CREATED, bookingService.createBooking(requestDto));

    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingFindResponseDto>> getBookingById(@PathVariable Long bookingId){
        return ApiResponse.success(HttpStatus.OK, bookingService.findBookingById(bookingId));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingFindResponseDto>>> findBookings(){
        return ApiResponse.success(HttpStatus.OK, bookingService.findBookings());
    }

    @PatchMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingFindResponseDto>> updateBooking(
            @PathVariable Long bookingId, @RequestBody BookingUpdateRequestDto requestDto) {
        bookingService.updateBooking(bookingId,requestDto);
        return ApiResponse.success(HttpStatus.OK,"수정이 완료되었습니다" );
    }

    @DeleteMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingFindResponseDto>> deleteBooking(@PathVariable Long bookingId) {
        bookingService.deleteBooking(bookingId);
        return ApiResponse.success(HttpStatus.OK,"삭제가 완료되었습니다" );
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<BookingFindResponseDto>>> findBookingsByService(
            @RequestBody BookingFindByServiceDto requestDto) {
        return ApiResponse.success(HttpStatus.OK, bookingService.findBookingsByService(requestDto));

    }




}
