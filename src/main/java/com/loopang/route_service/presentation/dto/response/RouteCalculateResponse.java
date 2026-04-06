package com.loopang.route_service.presentation.dto.response;

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
