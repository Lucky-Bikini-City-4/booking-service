package com.dayaeyak.booking.domain.detail.dto.request;

import com.dayaeyak.booking.domain.detail.payload.BookingDetailPayload;

public record BookingDetailUpdateRequestDto(
        Long bookingId,
        BookingDetailPayload details
) {}