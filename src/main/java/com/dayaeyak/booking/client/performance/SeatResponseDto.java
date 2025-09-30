package com.dayaeyak.booking.client.performance;

public record SeatResponseDto(
        Long performanceId,
        Long sessionId,
        Long sectionId,
        Long seatId,
        Integer seatNumber,
        Boolean isSoldOut
) {
}
