package com.dayaeyak.booking.client.test;

import org.springframework.stereotype.Component;

@Component
public interface PaymentClasdasdsaient {
    class PaymentResult {
        private final boolean success;
        private final String message;
        private final boolean partialCaptured;

        public PaymentResult(boolean success, String message, boolean partialCaptured) {
            this.success = success;
            this.message = message;
            this.partialCaptured = partialCaptured;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public boolean isPartialCaptured() { return partialCaptured; }
    }

    /**
     * 결제 요청 (동기) - 실제 API에 맞게 파라미터 조정
     */
    PaymentResult requestPayment(Long bookingId, Long userId, Integer amount);

    /**
     * 환불(동기)
     */
    void refund(Long bookingId);
}