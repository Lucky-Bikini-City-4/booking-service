package com.dayaeyak.booking.domain.booking.dto.kafka;

import com.dayaeyak.booking.domain.booking.enums.ServiceType;

import java.time.LocalDateTime;

public record BookingRequestKafkaDto(
        Long userId,
        ServiceType serviceType,
        Long serviceId,
        String userName,
        String serviceName,
        LocalDateTime date
) {
}
