package com.example.newsdatacollector.client;

import com.example.newsdatacollector.dto.NewsDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.regex.Pattern;

@Slf4j
@Component
public class HackerNewsApiClient {

    private static final Pattern AI_PATTERN = Pattern.compile(
            "\\b(ai|llm|gpt|gpt-4|gpt-5|chatgpt|machine learning|deep learning|" +
            "neural network|openai|anthropic|gemini|claude|artificial intelligence|" +
            "large language model|generative ai|stable diffusion|midjourney|copilot|" +
            "transformer|embedding|fine.?tun|rag|vector db)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://hacker-news.firebaseio.com/v0")
            .build();

    public Flux<NewsDto> fetchNewAiPosts() {
        return webClient.get()
                .uri("/newstories.json")
                .retrieve()
                .bodyToMono(Long[].class)
                .flatMapMany(ids -> Flux.fromArray(ids).take(50))
                .flatMap(this::fetchItem, 10)
                .filter(item -> item.getTitle() != null && isAiRelated(item.getTitle()))
                .map(item -> NewsDto.builder()
                        .source("hackernews")
                        .keyword(extractKeyword(item.getTitle()))
                        .title(item.getTitle())
                        .url(item.getUrl())
                        .author(item.getBy())
                        .publishedAt(Instant.ofEpochSecond(item.getTime()).toString())
                        .build())
                .doOnError(e -> log.error("Hacker News API 호출 실패: {}", e.getMessage()));
    }

    private Flux<HnItem> fetchItem(Long id) {
        return webClient.get()
                .uri("/item/{id}.json", id)
                .retrieve()
                .bodyToMono(HnItem.class)
                .flux()
                .onErrorResume(e -> {
                    log.warn("아이템 조회 실패 id={}: {}", id, e.getMessage());
                    return Flux.empty();
                });
    }

    private boolean isAiRelated(String title) {
        return AI_PATTERN.matcher(title).find();
    }

    private String extractKeyword(String title) {
        java.util.regex.Matcher matcher = AI_PATTERN.matcher(title);
        return matcher.find() ? matcher.group().toLowerCase() : "ai";
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class HnItem {
        private Long id;
        private String title;
        private String url;
        private String by;
        private long time;
        private int score;
    }
}
