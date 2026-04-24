package com.example.newsdatacollector.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NewsDto {

    private String source;
    private String keyword;
    private String title;
    private String url;
    private String content;
    private String author;
    private String publishedAt;
}
