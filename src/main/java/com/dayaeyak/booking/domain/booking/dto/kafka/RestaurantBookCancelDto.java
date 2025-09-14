package com.dayaeyak.booking.domain.booking.dto.kafka;

import com.dayaeyak.booking.domain.booking.enums.ServiceType;

public record RestaurantBookCancelDto(
        Long userId,
        ServiceType serviceType,
        Long serviceId,
        String restaurantName,
        String userName,
        String reason
) {
}