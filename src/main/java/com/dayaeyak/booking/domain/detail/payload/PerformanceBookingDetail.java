package com.dayaeyak.booking.domain.detail.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceBookingDetail implements BookingDetailPayload {
    private String sectionInfo;
    private int seatNumber;
    private LocalDate session;
    private int seatPrice;
}
