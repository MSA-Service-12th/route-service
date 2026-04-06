package com.loopang.route_service.domain.service.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * 경로 계산 결과 — 도메인/애플리케이션 계층용 순수 모델.
 * presentation 계층에서 RouteCalculateResponse로 매핑된다.
 */
@Builder
public record RouteCalculationResult(
        UUID fromHubId,
        UUID toHubId,
        List<PathNode> path,
        List<RouteEdge> routeEdges,
        double totalDistance,
        double totalDuration
) {

    @Builder
    public record PathNode(int sequence, UUID hubId) {}

    @Builder
    public record RouteEdge(
            int sequence,
            UUID fromHubId,
            UUID toHubId,
            double distance,
            double duration
    ) {}
}
