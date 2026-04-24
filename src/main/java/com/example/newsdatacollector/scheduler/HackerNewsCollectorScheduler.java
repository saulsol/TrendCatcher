package com.example.newsdatacollector.scheduler;

import com.example.newsdatacollector.client.HackerNewsApiClient;
import com.example.newsdatacollector.producer.NewsKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HackerNewsCollectorScheduler {

    private final HackerNewsApiClient hackerNewsApiClient;
    private final NewsKafkaProducer producer;

    @Scheduled(initialDelay = 0, fixedDelay = 5 * 60 * 1000)
    public void collect() {
        log.info("[HackerNews] 포스트 수집 시작");
        hackerNewsApiClient.fetchNewAiPosts()
                .subscribe(
                        news -> {
                            log.info("[HackerNews] title={}", news.getTitle());
                            producer.send(news);
                        },
                        e -> log.error("[HackerNews] 수집 오류: {}", e.getMessage()),
                        () -> log.info("[HackerNews] 수집 완료")
                );
    }
}
