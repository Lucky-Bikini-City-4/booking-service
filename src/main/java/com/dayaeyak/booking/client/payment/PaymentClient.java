package com.dayaeyak.booking.client.payment;

import com.dayaeyak.booking.utils.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("payment-service")
public interface PaymentClient {

    @PostMapping("/payments/request")
    ApiResponse<PaymentRequestResponseDto> requestPayment(@RequestBody PaymentCreateRequestDto requestDto);

}
