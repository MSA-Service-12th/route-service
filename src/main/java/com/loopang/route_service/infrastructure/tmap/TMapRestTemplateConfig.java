package com.loopang.route_service.infrastructure.tmap;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class TMapRestTemplateConfig {

    /**
     * TMap 전용 RestTemplate. 스케줄러 스레드가 무한정 블로킹되지 않도록 timeout을 명시적으로 설정.
     */
    @Bean
    public RestTemplate tmapRestTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(5))
                .build();
    }
}
