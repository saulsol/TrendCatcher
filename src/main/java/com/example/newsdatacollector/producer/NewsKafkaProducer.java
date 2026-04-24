package com.example.newsdatacollector.producer;

import com.example.newsdatacollector.deduplication.NewsDeduplicator;
import com.example.newsdatacollector.dto.NewsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsKafkaProducer {

    private final KafkaTemplate<String, NewsDto> kafkaTemplate;
    private final NewsDeduplicator deduplicator;

    @Value("${kafka.topic.news-korean}")
    private String koreanTopic;

    @Value("${kafka.topic.news-global}")
    private String globalTopic;

    public void send(NewsDto news) {
        if (deduplicator.isDuplicate(news.getUrl())) {
            log.debug("중복 스킵 [{}] title={}", news.getSource(), news.getTitle());
            return;
        }

        String topic = "naver".equals(news.getSource()) ? koreanTopic : globalTopic;
        kafkaTemplate.send(topic, news.getUrl(), news)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka 전송 실패 [{}] url={}: {}", news.getSource(), news.getUrl(), ex.getMessage());
                    } else {
                        deduplicator.markAsSeen(news.getUrl());
                        log.info("Kafka 전송 성공 [{}→{}] title={}", news.getSource(), topic, news.getTitle());
                    }
                });
    }
}
