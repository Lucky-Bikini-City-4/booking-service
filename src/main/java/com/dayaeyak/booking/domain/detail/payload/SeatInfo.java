package com.dayaeyak.booking.domain.detail.payload;

public record SeatInfo(
        Long seatId,
        int seatNumber,
        int price
) { }