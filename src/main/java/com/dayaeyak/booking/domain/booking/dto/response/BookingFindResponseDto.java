package com.dayaeyak.booking.domain.booking.dto.response;

import com.dayaeyak.booking.domain.booking.Booking;
import com.dayaeyak.booking.domain.booking.enums.BookingStatus;
import com.dayaeyak.booking.domain.booking.enums.ServiceType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDateTime;

public record BookingFindResponseDto(
        Long bookingId,
        Long userId,
        Long serviceId,
        ServiceType serviceType,
        Integer totalFee,
        BookingStatus status,
        LocalDateTime cancelDeadline,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
    public static BookingFindResponseDto from(Booking booking) {
        return new BookingFindResponseDto(
                booking.getId(),
                booking.getUserId(),
                booking.getServiceId(),
                booking.getServiceType(),
                booking.getTotalFee(),
                booking.getStatus(),
                booking.getCancelDeadline(),
                booking.getCreatedAt(),
                booking.getUpdatedAt(),
                booking.getDeletedAt()
        );
    }

}
