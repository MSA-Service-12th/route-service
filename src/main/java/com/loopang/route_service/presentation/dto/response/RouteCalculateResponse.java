package com.loopang.route_service.presentation.dto.response;

import com.loopang.route_service.domain.service.dto.RouteCalculationResult;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class RouteCalculateResponse {

    private UUID fromHubId;
    private UUID toHubId;
    private List<PathNode> path;
    private List<RouteEdge> routeEdges;
    private Double totalDistance;
    private Double totalDuration;

    public static RouteCalculateResponse from(RouteCalculationResult result) {
        return RouteCalculateResponse.builder()
                .fromHubId(result.fromHubId())
                .toHubId(result.toHubId())
                .path(result.path().stream()
                        .map(n -> PathNode.builder().sequence(n.sequence()).hubId(n.hubId()).build())
                        .toList())
                .routeEdges(result.routeEdges().stream()
                        .map(e -> RouteEdge.builder()
                                .sequence(e.sequence())
                                .fromHubId(e.fromHubId())
                                .toHubId(e.toHubId())
                                .distance(e.distance())
                                .duration(e.duration())
                                .build())
                        .toList())
                .totalDistance(result.totalDistance())
                .totalDuration(result.totalDuration())
                .build();
    }

    @Getter
    @Builder
    public static class PathNode {
        private int sequence;
        private UUID hubId;
    }

    @Getter
    @Builder
    public static class RouteEdge {
        private int sequence;
        private UUID fromHubId;
        private UUID toHubId;
        private Double distance;
        private Double duration;
    }
}
