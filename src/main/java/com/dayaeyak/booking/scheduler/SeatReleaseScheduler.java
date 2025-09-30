package com.dayaeyak.booking.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatReleaseScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<Long> releaseSeats; 
    private static final String SEAT_RELEASE_QUEUE = "seat:release:queue";

    // 5초마다 실행
    //@Scheduled(fixedRate = 5000)
    public void processSeatReleaseQueue() {
        long now = System.currentTimeMillis();

        // 1. 실행 시간이 된 작업들을 Redis Sorted Set에서 가져옴
        Set<String> jobs = redisTemplate.opsForZSet().rangeByScore(SEAT_RELEASE_QUEUE, 0, now);

        if (jobs == null || jobs.isEmpty()) {
            return;
        }

        log.info("Found {} seat(s) to release.", jobs.size());

        for (String jobInfo : jobs) {
            try {
                // 2. 작업 정보 파싱 (예: "performanceId:sessionId:sectionId:seatId")
                String[] parts = jobInfo.split(":");
                if (parts.length < 4) {
                    log.error("Invalid job format: {}", jobInfo);
                    // 잘못된 형식의 작업은 큐에서 제거
                    redisTemplate.opsForZSet().remove(SEAT_RELEASE_QUEUE, jobInfo);
                    continue;
                }
                
                String performanceId = parts[0];
                String sessionId = parts[1];
                String sectionId = parts[2];
                String seatId = parts[3];
                String redisKey = String.format("seat:%s:%s:%s", performanceId, sessionId, sectionId);

                // 3. Lua 스크립트 실행하여 좌석 상태를 'available'로 변경
                // releaseSeats 스크립트는 'locked' 상태의 좌석을 'available'로 바꾼다고 가정
                redisTemplate.execute(releaseSeats, Collections.singletonList(redisKey), seatId);

                // 4. 처리 완료된 작업을 큐에서 삭제
                redisTemplate.opsForZSet().remove(SEAT_RELEASE_QUEUE, jobInfo);

                log.info("Successfully released seat lock for job: {}", jobInfo);

            } catch (Exception e) {
                log.error("Failed to process seat release job: {}", jobInfo, e);
                // 실패한 작업은 일단 큐에 남겨두거나, 별도의 '실패 큐'로 옮겨서 수동으로 처리할 수 있습니다.
            }
        }
    }
}
