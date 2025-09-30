package com.dayaeyak.booking.domain.booking.dto.request;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public record BookingSeatRequestDto(
        Long performanceId,
        Long sessionId, // 레디스 선점
        Long sectionId,
        Long seatId,
        Integer seatNumber,
        String sectionName,
        int seatPrice,
        LocalDateTime dateTime
) {
    public static List<BookingSeatRequestDto> from(BookingPerformanceRequestDto performanceRequest) {
        LocalDateTime sessionDateTime = performanceRequest.sessionDate().atTime(performanceRequest.sessionTime());
        List<BookingSeatRequestDto> seatRequests = new ArrayList<>();
        List<Long> seatIds = performanceRequest.seatIds();
        List<Integer> seatNumbers = performanceRequest.seatNumber();

        if (seatIds.size() != seatNumbers.size()) {
            throw new IllegalArgumentException("seatIds 와 seatNumbers 리스트 크기가 다릅니다.");
        }

        for (int i = 0; i < seatIds.size(); i++) {
            seatRequests.add(new BookingSeatRequestDto(
                    performanceRequest.performanceId(),
                    performanceRequest.sessionId(),
                    performanceRequest.sectionId(),
                    seatIds.get(i),
                    seatNumbers.get(i),
                    performanceRequest.sectionName(),
                    performanceRequest.seatPrice(),
                    sessionDateTime
            ));
        }
        return seatRequests;
    }
}