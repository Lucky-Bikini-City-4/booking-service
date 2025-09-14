package com.dayaeyak.booking.domain.detail.payload;

import com.dayaeyak.booking.domain.detail.enums.Grade;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExhibitionBookingDetail implements BookingDetailPayload {
    private Grade grade;
    private int price;
}
