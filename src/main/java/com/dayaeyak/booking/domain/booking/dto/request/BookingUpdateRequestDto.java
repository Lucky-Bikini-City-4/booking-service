package com.dayaeyak.booking.domain.booking.dto.request;

import com.dayaeyak.booking.domain.booking.enums.BookingStatus;
import com.dayaeyak.booking.domain.booking.enums.ServiceType;

import java.time.LocalDateTime;

public record BookingUpdateRequestDto(
        Long userId,
        Long serviceId,
        ServiceType serviceType, // EXHIBITION, PERFORMANCE, RESTAURANT
        Integer totalFee,
        BookingStatus status,
        LocalDateTime cancelDeadline

) {
}
