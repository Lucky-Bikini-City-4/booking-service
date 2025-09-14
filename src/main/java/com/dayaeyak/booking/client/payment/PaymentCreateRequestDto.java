package com.dayaeyak.booking.client.payment;

import com.dayaeyak.booking.domain.booking.Booking;
import com.dayaeyak.booking.domain.booking.dto.response.BookingFindResponseDto;

public record PaymentCreateRequestDto(
        Long bookingId,
        PaymentStatus status,
        int fee
) {
    public static PaymentCreateRequestDto from(Long bookingId, PaymentStatus status, int fee) {
        return new PaymentCreateRequestDto(bookingId, status, fee);
    }

}