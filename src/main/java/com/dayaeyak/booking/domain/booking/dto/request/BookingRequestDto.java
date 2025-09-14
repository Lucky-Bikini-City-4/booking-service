package com.dayaeyak.booking.domain.booking.dto.request;

import com.dayaeyak.booking.domain.booking.enums.BookingStatus;
import com.dayaeyak.booking.domain.booking.enums.ServiceType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

public record BookingRequestDto(
        //공통 부분
        Long userId,
        Long serviceId,
        ServiceType serviceType,
        int totalFee,
        BookingStatus status,
        @JsonTypeInfo(
                use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY, // 'serviceType'을 기준으로 분기
                property = "serviceType"
        )
        @JsonSubTypes({
                @JsonSubTypes.Type(value = BookingPerformanceRequestDto.class, name = "PERFORMANCE"),
                @JsonSubTypes.Type(value = BookingExhibitionRequestDto.class, name = "EXHIBITION"),
                @JsonSubTypes.Type(value = BookingRestaurantRequestDto.class, name = "RESTAURANT")
        })
        BookingDetailRequest bookingDetailRequest
) {
}
