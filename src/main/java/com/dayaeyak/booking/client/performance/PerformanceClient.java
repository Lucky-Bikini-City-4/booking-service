package com.dayaeyak.booking.client.performance;

import com.dayaeyak.booking.domain.booking.dto.kafka.SeatStatusDto;
import com.dayaeyak.booking.utils.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient("performance-service")
public interface PerformanceClient {


    @GetMapping("/performances/{performanceId}/sessions/{sessionId}/sections/{sectionId}/seats/{seatId}")
    public ApiResponse<SeatResponseDto> readPerformanceSeat(
            @PathVariable Long performanceId,
            @PathVariable Long sessionId,
            @PathVariable Long sectionId,
            @PathVariable Long seatId);

    @PostMapping("/performances/{performanceId}/sessions/{sessionId}/sections/{sectionId}/seats/{seatId}")
    public ApiResponse<SeatResponseDto> changeIsSoldOut(
            @PathVariable Long performanceId,
            @PathVariable Long sessionId,
            @PathVariable Long sectionId,
            @PathVariable Long seatId,
            @RequestBody UpdateSeatSoldOutRequestDto requestDto);

    @GetMapping("/performances/{performanceId}/sessions/{sessionId}/sections/{sectionId}/seats")
    public ApiResponse<List<SeatStatusDto>> readPerformanceSeats(
            @PathVariable Long performanceId,
            @PathVariable Long sessionId,
            @PathVariable Long sectionId);

    }





