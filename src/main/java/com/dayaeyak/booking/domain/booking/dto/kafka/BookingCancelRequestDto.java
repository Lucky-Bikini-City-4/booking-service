package com.dayaeyak.booking.domain.booking.dto.kafka;

import com.dayaeyak.booking.domain.booking.enums.ServiceType;

public record BookingCancelRequestDto(
        Long userId,
        ServiceType serviceType,
        Long serviceId,
        String serviceName
) {
}