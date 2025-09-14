package com.dayaeyak.booking.domain.detail.dto.request;

import com.dayaeyak.booking.domain.detail.payload.BookingDetailPayload;
import jakarta.validation.constraints.NotNull;

public record BookingDetailCreateRequestDto(
        @NotNull
        Long bookingId,
        @NotNull
        BookingDetailPayload details
) {}