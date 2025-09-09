package com.dayaeyak.booking.domain.booking.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BookingStatus {

    PENDING("예약중"),
    COMPLETED("완료"),
    CANCELLED("취소"),
    FAILED("실패");

    private final String status;
}
