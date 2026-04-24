package com.example.newsdatacollector.scheduler;

import com.example.newsdatacollector.client.NaverNewsApiClient;
import com.example.newsdatacollector.config.NewsProperties;
import com.example.newsdatacollector.producer.NewsKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverNewsCollectorScheduler {

    private final NaverNewsApiClient naverNewsApiClient;
    private final NewsKafkaProducer producer;
    private final NewsProperties newsProperties;

    @Scheduled(initialDelay = 0, fixedDelay = 10 * 60 * 1000)
    public void collect() {
        newsProperties.getKeywords().forEach(keyword -> {
            log.info("[Naver] 키워드='{}' 수집 시작", keyword);
            naverNewsApiClient.searchNews(keyword)
                    .subscribe(
                            news -> {
                                log.info("[Naver][{}] title={}", keyword, news.getTitle());
                                producer.send(news);
                            },
                            e -> log.error("[Naver][{}] 수집 오류: {}", keyword, e.getMessage()),
                            () -> log.info("[Naver][{}] 수집 완료", keyword)
                    );
        });
    }
}
