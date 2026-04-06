package com.loopang.route_service.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.loopang.route_service.domain.entity.HubRoute;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class RouteResponse {

    private UUID routeId;
    private UUID fromHubId;
    private UUID toHubId;
    private Double distance;
    private Double duration;

    // Lombok이 boolean isActive 필드에 생성하는 getter는 isActive()라 Jackson이 기본값으로 "active"로 직렬화한다.
    // 요청 DTO의 isActive 필드와 대칭 맞추기 위해 JSON 이름을 명시적으로 고정한다.
    @JsonProperty("isActive")
    private boolean isActive;

    private LocalDateTime createdAt;

    public static RouteResponse from(HubRoute route) {
        return RouteResponse.builder()
                .routeId(route.getId())
                .fromHubId(route.getFromHubId())
                .toHubId(route.getToHubId())
                .distance(route.getDistance())
                .duration(route.getDuration())
                .isActive(route.isActive())
                .createdAt(route.getCreatedAt())
                .build();
    }
}
