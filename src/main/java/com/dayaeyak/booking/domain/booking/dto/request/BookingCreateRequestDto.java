package com.dayaeyak.booking.domain.booking.dto.request;

import com.dayaeyak.booking.domain.booking.enums.BookingStatus;
import com.dayaeyak.booking.domain.booking.enums.ServiceType;

public record BookingCreateRequestDto(
        Long userId,
        Long serviceId,
        ServiceType serviceType, // EXHIBITION, PERFORMANCE, RESTAURANT
        Integer totalFee,
        BookingStatus status // PENDING, COMPLETED, CANCELLED, FAILED
    ){}
