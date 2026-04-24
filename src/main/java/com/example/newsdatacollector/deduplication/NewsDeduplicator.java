package com.example.newsdatacollector.deduplication;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class NewsDeduplicator {

    private static final long TTL_HOURS = 24;

    // url → 최초 수집 시각
    private final ConcurrentHashMap<String, Instant> seenUrls = new ConcurrentHashMap<>();

    public boolean isDuplicate(String url) {
        if (url == null) return false;
        return seenUrls.containsKey(url);
    }

    public void markAsSeen(String url) {
        if (url != null) {
            seenUrls.put(url, Instant.now());
        }
    }

    // 24시간 지난 항목 자동 정리
    @Scheduled(fixedDelay = 60 * 60 * 1000)
    public void cleanup() {
        Instant threshold = Instant.now().minusSeconds(TTL_HOURS * 3600);
        int before = seenUrls.size();
        seenUrls.entrySet().removeIf(entry -> entry.getValue().isBefore(threshold));
        log.info("중복 캐시 정리: {}건 → {}건", before, seenUrls.size());
    }
}
