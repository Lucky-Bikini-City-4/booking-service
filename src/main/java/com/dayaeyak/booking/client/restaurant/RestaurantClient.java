package com.dayaeyak.booking.client.restaurant;

import com.dayaeyak.booking.utils.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@FeignClient("restaurant-service")
public interface RestaurantClient {

    @GetMapping("/restaurantSeats/{restaurantId}")
    public ApiResponse<SeatAvailabilityDto> getSeats(
            @PathVariable Long restaurantId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    );

    @GetMapping("/restaurantSeats/reserve")
    public ApiResponse<SeatAvailabilityDto> reserveSeats(
            @RequestBody SeatsRequestDto request
    );

}
