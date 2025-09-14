package com.dayaeyak.booking.domain.booking.dto.kafka;

import com.dayaeyak.booking.domain.booking.enums.ServiceType;

import java.time.LocalDateTime;

public record RestaurantBookConfirmDto(
        Long userId,
        ServiceType serviceType,
        Long serviceId,
        String restaurantName,
        String userName,
        Integer people,
        LocalDateTime date
) {
}