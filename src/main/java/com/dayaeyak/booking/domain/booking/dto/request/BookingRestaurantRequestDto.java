package com.dayaeyak.booking.domain.booking.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

public record BookingRestaurantRequestDto(
        Long restaurantId,
        int customer,
        LocalDate date,
        LocalTime time

) implements BookingDetailRequest {
}
