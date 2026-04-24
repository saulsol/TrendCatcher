package com.example.newsdatacollector.client;

import com.example.newsdatacollector.dto.NewsDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class NaverNewsApiClient {

    private static final Pattern AI_PATTERN = Pattern.compile(
            "인공지능|AI|LLM|GPT|챗GPT|머신러닝|딥러닝|뉴럴|오픈AI|앤트로픽|제미나이|클로드|" +
            "생성형|거대언어모델|파운데이션모델|코파일럿|자율주행|로봇|데이터센터|반도체|엔비디아",
            Pattern.CASE_INSENSITIVE
    );

    @Value("${naver.client-id}")
    private String clientId;

    @Value("${naver.client-secret}")
    private String clientSecret;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://openapi.naver.com")
            .build();

    public Flux<NewsDto> searchNews(String query) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/search/news.json")
                        .queryParam("query", query)
                        .queryParam("display", 100)
                        .queryParam("sort", "date")
                        .build())
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .retrieve()
                .bodyToMono(SearchResponse.class)
                .flatMapMany(response -> Flux.fromIterable(response.getItems()))
                .filter(item -> isAiRelated(item.getTitle()) || isAiRelated(item.getDescription()))
                .map(item -> NewsDto.builder()
                        .source("naver")
                        .keyword(query)
                        .title(item.getTitle().replaceAll("<[^>]*>", ""))
                        .url(item.getOriginallink())
                        .content(item.getDescription().replaceAll("<[^>]*>", ""))
                        .publishedAt(item.getPubDate())
                        .build())
                .doOnError(e -> log.error("네이버 뉴스 API 호출 실패: {}", e.getMessage()));
    }

    private boolean isAiRelated(String text) {
        if (text == null) return false;
        return AI_PATTERN.matcher(text).find();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SearchResponse {
        private List<NaverItem> items;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class NaverItem {
        private String title;
        private String originallink;
        private String link;
        private String description;
        private String pubDate;
    }
}
