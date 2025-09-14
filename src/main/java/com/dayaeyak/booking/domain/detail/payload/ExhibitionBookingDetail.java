package com.dayaeyak.booking.domain.detail.payload;

import com.dayaeyak.booking.domain.booking.enums.ServiceType;
import com.dayaeyak.booking.enums.Grade;
import com.dayaeyak.booking.enums.Grade;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


public record ExhibitionBookingDetail(
        ServiceType type,
        Grade grade,
        int price

) implements BookingDetailPayload {

}
