# TrendCatcher

AI 뉴스를 실시간으로 수집해 Kafka로 스트리밍하고 Elasticsearch에 저장하는 뉴스 파이프라인

## 프로젝트 개요

네이버 뉴스 API와 HackerNews를 통해 AI 관련 한국어/해외 뉴스를 수집하고, Apache Kafka로 스트리밍하여 Elasticsearch에 키워드 기반으로 인덱싱하는 실시간 뉴스 파이프라인입니다. Spring Boot WebFlux 기반의 비동기 수집, URL 중복 제거, 한국어 형태소 분석(nori)을 지원하며 Kibana 대시보드로 트렌드를 시각화합니다.

## 아키텍처

<img width="600" height="350" alt="trendCatcher" src="https://github.com/user-attachments/assets/4fea0be1-24fd-4a45-81f2-2597998d3c2b" />


## 기술 스택

| 분류 | 기술 |
|------|------|
| Backend | Spring Boot 3.x, Spring WebFlux |
| Message Queue | Apache Kafka (KRaft mode) |
| Pipeline | Logstash |
| Search & Storage | Elasticsearch 8.x |
| Visualization | Kibana |
| 외부 API | Naver News API, HackerNews Firebase API |


## 결과 및 주요 기능
<img width="600" height="300" alt="image" src="https://github.com/user-attachments/assets/b5b88cac-ac71-4eac-887b-9e74b840438d" />

- **한국어/해외 뉴스 동시 수집** — 네이버 뉴스(한국어), HackerNews(영어) AI 관련 뉴스 수집
- **키워드 기반 인덱스 분리** — `naver-인공지능`, `hackernews-llm` 등 키워드별 Elasticsearch 인덱스 자동 생성
- **URL 중복 제거** — 24시간 TTL 기반 인메모리 중복 필터링
- **한국어 형태소 분석** — nori analyzer 적용으로 한국어 검색 품질 향상
- **Kafka 토픽 분리** — `news-korean`(한국어), `news-global`(해외) 토픽으로 분리 처리
- **인덱스 템플릿 자동 초기화** — 애플리케이션 시작 시 Elasticsearch 인덱스 템플릿 자동 생성

## 수집 키워드

`인공지능` `AI` `LLM` `반도체` `챗GPT`

## 수집 주기

| 소스 | 주기 |
|------|------|
| 네이버 뉴스 | 10분 |
| HackerNews | 5분 |

## 환경 변수

```
NAVER_CLIENT_ID=
NAVER_CLIENT_SECRET=
ES_USERNAME=elastic
ES_PASSWORD=elastic
```

## 성능 튜닝 및 개선 포인트

- **Kafka 파티션 3개 적용** — `news-korean`, `news-global` 토픽 각 3개 파티션으로 구성
- **Logstash consumer_threads 3개** — 파티션 수에 맞춰 병렬 컨슈머 스레드 설정
- 현재는 프로듀서가 단일 인스턴스이고 수집량이 적어 체감 성능 차이는 미미하나, 수집 소스 또는 키워드 확장 시 병렬 처리 효과를 기대할 수 있는 구조로 설계

## Log Stash Config

```
input {
  kafka {
    bootstrap_servers => "localhost:9092"
    topics => ["news-korean", "news-global"]
    codec => "json"
    group_id => "log-consumer-test"
    auto_offset_reset => "earliest"
    fetch_min_bytes => "1"
    fetch_max_wait_ms => "500"
    consumer_threads => 3
  }
}
filter {
  mutate{
    lowercase => ["keyword", "source"]
    gsub => ["keyword", " ", "-"]
  }
  date {
    match => ["publishedAt", "ISO8601", "EEE, dd MMM yyyy HH:mm:ss Z"]
    target => "@timestamp"
  }
}
output {
  elasticsearch {
    hosts => ["https://localhost:9200"]
    index => "%{source}-%{keyword}"
    user => "elastic"
    password => "elastic"
    ssl_enabled => true
    ssl_certificate_authorities => ["~"]
  }
}

```







