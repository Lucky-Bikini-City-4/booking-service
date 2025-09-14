package com.dayaeyak.booking.domain.detail.payload;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ExhibitionBookingDetail.class, name = "EXHIBITION"),
        @JsonSubTypes.Type(value = PerformanceBookingDetail.class, name = "PERFORMANCE"),
        @JsonSubTypes.Type(value = RestaurantBookingDetail.class, name = "RESTAURANT")
})
public interface BookingDetailPayload {

}