package com.dayaeyak.booking.domain.detail.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Grade {
    ALL("전체 관람가"),
    R15("15세 이상 관람가"),
    R18("18세 이상 관람가");

    private final String grade;
}