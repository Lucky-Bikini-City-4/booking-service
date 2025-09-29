package com.dayaeyak.booking.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Grade {

    ALL("ALL"),
    R15_PLUS("R15+"),
    R18_PLUS("R18+");

    private final String value;
}


