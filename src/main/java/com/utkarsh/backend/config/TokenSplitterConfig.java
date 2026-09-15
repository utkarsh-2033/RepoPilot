package com.utkarsh.backend.config;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TokenSplitterConfig {

    @Value("${app.indexing.chunk-size:800}")
    private int chunkSize;

    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(chunkSize)
                .build();
    }
}
