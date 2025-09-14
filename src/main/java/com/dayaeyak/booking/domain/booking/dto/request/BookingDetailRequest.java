package com.dayaeyak.booking.domain.booking.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

// 각 서비스별 요청 DTO들이 구현할 마커 인터페이스

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXTERNAL_PROPERTY, // 상위 DTO의 serviceType을 기준으로 매핑
        property = "serviceType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = BookingPerformanceRequestDto.class, name = "PERFORMANCE"),
        @JsonSubTypes.Type(value = BookingExhibitionRequestDto.class, name = "EXHIBITION"),
        @JsonSubTypes.Type(value = BookingRestaurantRequestDto.class, name = "RESTAURANT")
})
public interface BookingDetailRequest {
}