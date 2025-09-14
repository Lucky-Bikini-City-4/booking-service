package com.dayaeyak.booking.domain.detail.dto.request;

import com.dayaeyak.booking.domain.detail.payload.BookingDetailPayload;

import java.util.List;

public record BookingDetailUpdateRequestDto(
        Long bookingId,
        List<BookingDetailPayload> details
) {}