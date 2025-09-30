package com.dayaeyak.booking.domain.detail.payload;

import com.dayaeyak.booking.domain.booking.enums.ServiceType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


public record PerformanceBookingDetail(
        Long performanceId,  // performanceId 추가
        Long sessionId,
        Long sectionId,
        String sectionName,
        LocalDateTime dateTime,
        List<SeatInfo> seatInfo
) implements BookingDetailPayload {

}
