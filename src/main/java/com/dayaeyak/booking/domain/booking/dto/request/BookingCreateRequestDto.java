package com.dayaeyak.booking.domain.booking.dto.request;

import com.dayaeyak.booking.domain.booking.enums.BookingStatus;
import com.dayaeyak.booking.domain.booking.enums.ServiceType;

public record BookingCreateRequestDto(
        Long userId,
        Long serviceId,
        ServiceType serviceType,
        int totalFee,
        BookingStatus status
) {}