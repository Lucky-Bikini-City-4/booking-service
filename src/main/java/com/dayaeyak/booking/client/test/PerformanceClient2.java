package com.dayaeyak.booking.client.test;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface PerformanceClient2 {
    /**
     * 공연 DB에서 좌석들이 여전히 사용 가능한지 확인 (true = 사용 가능)
     */
    boolean checkSeatsAvailable(Long serviceId, List<Integer> seatNumbers);

    /**
     * 공연 DB에 좌석을 판매(sold)로 반영. 성공 여부 반환
     */
    boolean markSeatsAsSold(Long serviceId, List<Integer> seatNumbers, Long bookingId);
}