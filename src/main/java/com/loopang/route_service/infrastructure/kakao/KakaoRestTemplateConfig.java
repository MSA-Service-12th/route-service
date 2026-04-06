package com.loopang.route_service.infrastructure.kakao;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class KakaoRestTemplateConfig {

    /**
     * Kakao Mobility Directions API 전용 RestTemplate.
     * 호출 스레드가 무한정 블로킹되지 않도록 timeout을 명시.
     */
    @Bean
    public RestTemplate kakaoRestTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(5))
                .build();
    }
}
