package com.loopang.route_service.infrastructure.scheduler;

import com.loopang.route_service.domain.entity.HubRoute;
import com.loopang.route_service.domain.repository.HubRouteRepository;
import com.loopang.route_service.domain.service.TMapProvider;
import com.loopang.route_service.domain.service.dto.HubData;
import com.loopang.route_service.infrastructure.client.HubFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TMapRefreshScheduler {

    private final HubRouteRepository hubRouteRepository;
    private final TMapProvider tMapProvider;
    private final HubFeignClient hubFeignClient;

    @Scheduled(fixedDelay = 3600000) // 1시간
    @Transactional
    public void refresh() {
        log.info("[TMapRefresh] 허브 라우트 갱신 시작");

        // Hub Service에서 허브 좌표 조회
        Map<UUID, double[]> hubCoords = new HashMap<>();
        try {
            HubFeignClient.HubListResponse response = hubFeignClient.getHubs();
            List<HubData> hubs = response.data();
            for (HubData hub : hubs) {
                hubCoords.put(hub.hubId(), new double[]{hub.latitude(), hub.longitude()});
            }
        } catch (Exception e) {
            log.error("[TMapRefresh] Hub Service 연결 실패 — 갱신 스킵");
            return;
        }

        Page<HubRoute> routes = hubRouteRepository.findAll(PageRequest.of(0, 300));
        int updated = 0;
        int failed = 0;

        for (HubRoute route : routes) {
            double[] from = hubCoords.get(route.getFromHubId());
            double[] to = hubCoords.get(route.getToHubId());

            if (from == null || to == null) {
                log.warn("[TMapRefresh] 좌표 없음: {} → {}", route.getFromHubId(), route.getToHubId());
                failed++;
                continue;
            }

            TMapProvider.TMapRouteResult result = tMapProvider.getRoute(from[0], from[1], to[0], to[1]);

            if (result != null) {
                route.refresh(result.distanceKm(), result.durationMin());
                updated++;
            } else {
                failed++;
            }

            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }

        log.info("[TMapRefresh] 갱신 완료: 성공 {}, 실패 {}", updated, failed);
    }
}
