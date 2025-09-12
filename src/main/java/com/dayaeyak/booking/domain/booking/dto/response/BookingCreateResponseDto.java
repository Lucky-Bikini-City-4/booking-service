package com.dayaeyak.booking.domain.booking.dto.response;

import com.dayaeyak.booking.domain.booking.Booking;

public record BookingCreateResponseDto(
        Long id
) {
    public static BookingCreateResponseDto from(Booking booking) {
        return new BookingCreateResponseDto(booking.getId());
    }
}


