package com.dayaeyak.booking.client.restaurant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SeatsRequestDto {
    private Long restaurantId;
    private LocalDate date;
    private int count;   //선택한 좌석
}