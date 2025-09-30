package com.dayaeyak.booking.domain.booking.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeatStatusDto {
    private Long seatId;
    private boolean isBooked;
}
