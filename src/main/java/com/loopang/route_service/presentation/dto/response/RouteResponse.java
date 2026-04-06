package com.loopang.route_service.presentation.dto.response;

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
