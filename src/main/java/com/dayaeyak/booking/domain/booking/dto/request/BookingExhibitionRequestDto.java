package com.dayaeyak.booking.domain.booking.dto.request;

import com.dayaeyak.booking.enums.Grade;

public record BookingExhibitionRequestDto(
        Grade grade,
        String name,
        int price
) implements BookingDetailRequest {

}
