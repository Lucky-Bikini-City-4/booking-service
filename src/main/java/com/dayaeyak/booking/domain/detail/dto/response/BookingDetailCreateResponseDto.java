package com.dayaeyak.booking.domain.detail.dto.response;

import com.dayaeyak.booking.domain.detail.BookingDetail;

public record BookingDetailCreateResponseDto(
        Long id
) {
    public static BookingDetailCreateResponseDto from(BookingDetail bookingDetail) {
        return new BookingDetailCreateResponseDto(bookingDetail.getId());
    }
}