package com.example.newsdatacollector.init;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.net.ssl.TrustManagerFactory;

@Slf4j
@Component
public class ElasticsearchIndexInitializer implements ApplicationRunner {

    private final WebClient webClient;

    public ElasticsearchIndexInitializer(
            @Value("${elasticsearch.host}") String esHost,
            @Value("${elasticsearch.username}") String username,
            @Value("${elasticsearch.password}") String password,
            @Value("${elasticsearch.ca-cert-path}") String caCertPath) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate caCert;
            try (FileInputStream fis = new FileInputStream(caCertPath)) {
                caCert = (X509Certificate) cf.generateCertificate(fis);
            }

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("elasticsearch-ca", caCert);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            SslContext sslContext = SslContextBuilder.forClient()
                    .trustManager(tmf)
                    .build();

            HttpClient httpClient = HttpClient.create()
                    .secure(spec -> spec.sslContext(sslContext));

            this.webClient = WebClient.builder()
                    .baseUrl(esHost)
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .defaultHeader("Content-Type", "application/json")
                    .defaultHeaders(h -> h.setBasicAuth(username, password))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("WebClient SSL 설정 실패", e);
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        createNaverTemplate();
        createHackerNewsTemplate();
    }

    private void createNaverTemplate() {
        String body = """
                {
                  "index_patterns": ["naver-*"],
                  "template": {
                    "settings": {
                      "analysis": {
                        "analyzer": {
                          "korean_analyzer": {
                            "type": "custom",
                            "tokenizer": "nori_tokenizer",
                            "filter": ["lowercase"]
                          }
                        }
                      }
                    },
                    "mappings": {
                      "properties": {
                        "source":      { "type": "keyword" },
                        "keyword":     { "type": "keyword" },
                        "title":       { "type": "text", "analyzer": "korean_analyzer" },
                        "content":     { "type": "text", "analyzer": "korean_analyzer" },
                        "url":         { "type": "keyword" },
                        "author":      { "type": "keyword" },
                        "publishedAt": { "type": "date", "format": "EEE, dd MMM yyyy HH:mm:ss Z" },
                        "@timestamp":  { "type": "date" }
                      }
                    }
                  }
                }
                """;

        webClient.put()
                .uri("/_index_template/naver-template")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                        res -> log.info("naver-template 생성 완료"),
                        err -> log.error("naver-template 생성 실패: {}", err.getMessage())
                );
    }

    private void createHackerNewsTemplate() {
        String body = """
                {
                  "index_patterns": ["hackernews-*"],
                  "template": {
                    "mappings": {
                      "properties": {
                        "source":      { "type": "keyword" },
                        "keyword":     { "type": "keyword" },
                        "title":       { "type": "text", "analyzer": "standard" },
                        "content":     { "type": "text", "analyzer": "standard" },
                        "url":         { "type": "keyword" },
                        "author":      { "type": "keyword" },
                        "publishedAt": { "type": "date", "format": "yyyy-MM-dd'T'HH:mm:ssX" },
                        "@timestamp":  { "type": "date" }
                      }
                    }
                  }
                }
                """;

        webClient.put()
                .uri("/_index_template/hackernews-template")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                        res -> log.info("hackernews-template 생성 완료"),
                        err -> log.error("hackernews-template 생성 실패: {}", err.getMessage())
                );
    }
}
