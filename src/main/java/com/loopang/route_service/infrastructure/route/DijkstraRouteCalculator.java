package com.loopang.route_service.infrastructure.route;

import com.loopang.route_service.domain.entity.HubRoute;
import com.loopang.route_service.domain.exception.RouteCalculationException;
import com.loopang.route_service.domain.repository.HubRouteRepository;
import com.loopang.route_service.domain.service.HubStatusProvider;
import com.loopang.route_service.domain.service.RouteCalculator;
import com.loopang.route_service.domain.service.dto.HubStatusData;
import com.loopang.route_service.domain.service.dto.RouteCalculationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DijkstraRouteCalculator implements RouteCalculator {

    private final HubRouteRepository hubRouteRepository;
    private final HubStatusProvider hubStatusProvider;

    // 혼잡도 가중 계수
    private static final double BUSY_WEIGHT = 0.8;   // 거리 80% 가중
    private static final double FULL_WEIGHT = 2.0;    // 거리 200% 가중

    @Override
    public RouteCalculationResult calculate(UUID fromHubId, UUID toHubId) {
        List<HubRoute> activeRoutes = hubRouteRepository.findAllByActiveTrue();

        // 허브 상태 캐시 (같은 계산 내 중복 호출 방지)
        Map<UUID, String> hubStatusCache = new HashMap<>();

        // 인접 리스트 구성
        Map<UUID, List<HubRoute>> graph = new HashMap<>();
        for (HubRoute route : activeRoutes) {
            graph.computeIfAbsent(route.getFromHubId(), k -> new ArrayList<>()).add(route);
        }

        // 다익스트라
        Map<UUID, Double> dist = new HashMap<>();
        Map<UUID, UUID> prev = new HashMap<>();
        Map<String, HubRoute> edgeMap = new HashMap<>();
        PriorityQueue<UUID> pq = new PriorityQueue<>(Comparator.comparingDouble(dist::get));

        dist.put(fromHubId, 0.0);
        pq.add(fromHubId);

        while (!pq.isEmpty()) {
            UUID current = pq.poll();
            double currentDist = dist.get(current);

            if (current.equals(toHubId)) break;

            List<HubRoute> neighbors = graph.getOrDefault(current, Collections.emptyList());
            for (HubRoute route : neighbors) {
                UUID next = route.getToHubId();

                // 혼잡도 가중 비용 계산
                double congestionWeight = getCongestionWeight(next, hubStatusCache);
                double weightedDistance = route.getDistance() * (1 + congestionWeight);

                double newDist = currentDist + weightedDistance;

                if (!dist.containsKey(next) || newDist < dist.get(next)) {
                    dist.put(next, newDist);
                    prev.put(next, current);
                    edgeMap.put(current + "->" + next, route);
                    pq.add(next);
                }
            }
        }

        if (!dist.containsKey(toHubId)) {
            throw new RouteCalculationException();
        }

        // 경로 역추적
        List<UUID> path = new ArrayList<>();
        UUID current = toHubId;
        while (current != null) {
            path.add(0, current);
            current = prev.get(current);
        }

        // 응답 구성 (실제 거리/시간은 원본 값)
        List<RouteCalculationResult.PathNode> pathNodes = new ArrayList<>();
        for (int i = 0; i < path.size(); i++) {
            pathNodes.add(RouteCalculationResult.PathNode.builder()
                    .sequence(i + 1)
                    .hubId(path.get(i))
                    .build());
        }

        List<RouteCalculationResult.RouteEdge> routeEdges = new ArrayList<>();
        double totalDistance = 0;
        double totalDuration = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            HubRoute edge = edgeMap.get(path.get(i) + "->" + path.get(i + 1));
            routeEdges.add(RouteCalculationResult.RouteEdge.builder()
                    .sequence(i + 1)
                    .fromHubId(edge.getFromHubId())
                    .toHubId(edge.getToHubId())
                    .distance(edge.getDistance())
                    .duration(edge.getDuration())
                    .build());
            totalDistance += edge.getDistance();
            totalDuration += edge.getDuration();
        }

        return RouteCalculationResult.builder()
                .fromHubId(fromHubId)
                .toHubId(toHubId)
                .path(pathNodes)
                .routeEdges(routeEdges)
                .totalDistance(totalDistance)
                .totalDuration(totalDuration)
                .build();
    }

    private double getCongestionWeight(UUID hubId, Map<UUID, String> cache) {
        String status = cache.computeIfAbsent(hubId, id -> {
            HubStatusData data = hubStatusProvider.getHubStatus(id);
            return data.status();
        });

        return switch (status) {
            case "혼잡" -> BUSY_WEIGHT;
            case "만원" -> FULL_WEIGHT;
            default -> 0.0; // 정상
        };
    }
}
