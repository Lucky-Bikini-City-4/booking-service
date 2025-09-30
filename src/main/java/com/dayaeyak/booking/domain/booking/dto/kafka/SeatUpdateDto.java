package com.dayaeyak.booking.domain.booking.dto.kafka;

import com.dayaeyak.booking.domain.booking.Booking;
import com.dayaeyak.booking.domain.booking.dto.request.BookingPerformanceRequestDto;
import com.dayaeyak.booking.domain.booking.dto.request.BookingSeatRequestDto;
import com.dayaeyak.booking.domain.booking.dto.response.BookingFindResponseDto;

/**
 * 공연 서비스로 좌석 상태 변경을 요청하기 위한 DTO
 * @param performanceId 공연 ID
 * @param sessionId 공연 회차 ID
 * @param sectionId 좌석 구역 ID
 * @param seatId 좌석 ID
 * @param isReserved 예약 여부 (true: 예약됨, false: 예약 취소)
 */
public record SeatUpdateDto(
    Long performanceId,
    Long sessionId,
    Long sectionId,
    Long seatId,
    boolean isReserved
) {
    public static SeatUpdateDto from(BookingSeatRequestDto dto) {
        return new SeatUpdateDto(
                dto.performanceId(),
                dto.sessionId(),
                dto.sectionId(),
                dto.seatId(),
                true
        );
    }
}
