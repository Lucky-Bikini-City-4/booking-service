package com.dayaeyak.booking.domain.booking.dto.kafka;

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