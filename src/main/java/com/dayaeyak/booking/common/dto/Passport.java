package com.dayaeyak.booking.common.dto;


import com.dayaeyak.booking.common.enums.UserRole;

public record Passport(
        Long userId,

        UserRole role
) {
}
