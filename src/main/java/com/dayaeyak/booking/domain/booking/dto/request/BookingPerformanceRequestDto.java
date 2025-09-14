package com.dayaeyak.booking.domain.booking.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record BookingPerformanceRequestDto(
        Long sessionId, // 레디스 선점
        LocalDate sessionDate, // 디테일 필요 정보
        LocalTime sessionTime, // 디테일 필요 정보
        String sectionName, // 디테일 필요정보
        int seatPrice, // 디테일 필요 정보
        List<Integer> seatNumber, // 선점 필요 정보
        boolean isSoldOut, // 선점 필요 정보
        Long performanceId,
        Long sectionId,
        Long seatId
) implements BookingDetailRequest {


}
