package com.dayaeyak.booking.domain.detail.dto.response;

import com.dayaeyak.booking.domain.detail.BookingDetail;
import com.dayaeyak.booking.domain.detail.payload.BookingDetailPayload;

import java.time.LocalDateTime;

public record BookingDetailFindResponseDto(
        Long id,
        Long bookingId,
        BookingDetailPayload details,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
    public static BookingDetailFindResponseDto from(BookingDetail bookingDetail) {
        return new BookingDetailFindResponseDto(
                bookingDetail.getId(),
                bookingDetail.getBookingId(),
                bookingDetail.getDetails(),
                bookingDetail.getCreatedAt(),
                bookingDetail.getUpdatedAt(),
                bookingDetail.getDeletedAt()
        );
    }
}