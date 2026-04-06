package com.loopang.route_service.infrastructure.kakao;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Kakao Mobility Directions API 호출 설정.
 * <p>application.yaml의 {@code kakao.rest-api-key} 값을 받아 보관한다.
 * 환경변수 {@code KAKAO_REST_API_KEY}로 주입하는 것을 권장한다.</p>
 */
@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "kakao")
public class KakaoProperties {

    @NotBlank(message = "kakao.rest-api-key는 필수입니다.")
    private String restApiKey;
}
