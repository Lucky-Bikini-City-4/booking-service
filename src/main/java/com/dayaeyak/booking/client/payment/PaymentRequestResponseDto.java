package com.dayaeyak.booking.client.payment;

import java.time.LocalDateTime;

public record PaymentRequestResponseDto(
        Long paymentId,
        PaymentStatus paymentStatus,
        Integer fee,
        LocalDateTime created_at,
        LocalDateTime updated_at,
        LocalDateTime delete_at

) {
}
