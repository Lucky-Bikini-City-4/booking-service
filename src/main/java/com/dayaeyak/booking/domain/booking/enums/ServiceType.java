package com.dayaeyak.booking.domain.booking.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum ServiceType {
    EXHIBITION("전시회"),
    PERFORMANCE("공연"),
    RESTAURANT("음식점"),
    ;


    private final String role;

}
