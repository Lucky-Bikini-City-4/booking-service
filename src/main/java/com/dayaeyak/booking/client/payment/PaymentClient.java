package com.dayaeyak.booking.client.payment;

import com.dayaeyak.booking.utils.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("payment-service")
public interface PaymentClient {

    @PostMapping("/payments/request")
    ApiResponse<PaymentRequestResponseDto> requestPayment(@RequestBody PaymentCreateRequestDto requestDto);

    @PostMapping("/payments/refund/{bookingId}")
    ApiResponse<Boolean> refundPayment(@PathVariable Long bookingId);



}
