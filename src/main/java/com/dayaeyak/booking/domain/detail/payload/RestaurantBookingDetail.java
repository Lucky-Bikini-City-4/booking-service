package com.dayaeyak.booking.domain.detail.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantBookingDetail implements BookingDetailPayload {
    private LocalDate date;
    private LocalTime time;
    private int guestCount;
}
