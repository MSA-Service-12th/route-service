package com.loopang.route_service.infrastructure.tmap;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "tmap")
public class TMapProperties {

    @NotBlank(message = "tmap.api-key는 필수입니다.")
    private String apiKey;
}
