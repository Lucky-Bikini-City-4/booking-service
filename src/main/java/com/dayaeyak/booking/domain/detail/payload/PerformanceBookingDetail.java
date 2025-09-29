package com.dayaeyak.booking.domain.detail.payload;

import com.dayaeyak.booking.domain.booking.enums.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;


public record PerformanceBookingDetail(
        ServiceType type,
        String sectionInfo,
        int seatNumber,
        LocalDateTime session,
        int seatPrice

) implements BookingDetailPayload {

}
