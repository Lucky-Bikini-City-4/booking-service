package com.dayaeyak.booking.client.restaurant;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SeatAvailabilityDto {
    private LocalDate date;
    private int availableSeats;
}