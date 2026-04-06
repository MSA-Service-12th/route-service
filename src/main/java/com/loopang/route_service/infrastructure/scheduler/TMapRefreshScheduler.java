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
import org.springframework.data.domain.Pageable;
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

    private static final int PAGE_SIZE = 100;
    private static final long CALL_DELAY_MS = 200L;

    private final HubRouteRepository hubRouteRepository;
    private final TMapProvider tMapProvider;
    private final HubFeignClient hubFeignClient;
    private final TMapRouteUpdater routeUpdater;

    @Scheduled(fixedDelay = 3600000) // 1시간
    public void refresh() {
        log.info("[TMapRefresh] 허브 라우트 갱신 시작");

        // 1. Hub Service에서 허브 좌표 조회 (트랜잭션 밖)
        Map<UUID, double[]> hubCoords;
        try {
            HubFeignClient.HubListResponse response = hubFeignClient.getHubs();
            List<HubData> hubs = response.data();
            hubCoords = new HashMap<>(hubs.size());
            for (HubData hub : hubs) {
                hubCoords.put(hub.hubId(), new double[]{hub.latitude(), hub.longitude()});
            }
        } catch (Exception e) {
            log.error("[TMapRefresh] Hub Service 연결 실패 — 갱신 스킵", e);
            return;
        }

        int updated = 0;
        int failed = 0;

        // 2. 모든 페이지를 순회 (첫 300건만 보는 버그 방지)
        int pageNumber = 0;
        boolean interrupted = false;

        while (!interrupted) {
            Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE);
            Page<HubRoute> routes = routeUpdater.loadPage(pageable);

            if (routes.isEmpty()) {
                break;
            }

            for (HubRoute route : routes) {
                double[] from = hubCoords.get(route.getFromHubId());
                double[] to = hubCoords.get(route.getToHubId());

                if (from == null || to == null) {
                    log.warn("[TMapRefresh] 좌표 없음: {} → {}", route.getFromHubId(), route.getToHubId());
                    failed++;
                    continue;
                }

                // TMap 외부 호출은 트랜잭션 밖에서 수행
                TMapProvider.TMapRouteResult result = tMapProvider.getRoute(from[0], from[1], to[0], to[1]);

                if (result == null) {
                    failed++;
                } else {
                    // 저장만 짧은 단건 트랜잭션으로 분리
                    try {
                        routeUpdater.applyRefresh(route.getId(), result.distanceKm(), result.durationMin());
                        updated++;
                    } catch (Exception e) {
                        log.warn("[TMapRefresh] 라우트 저장 실패: routeId={}", route.getId(), e);
                        failed++;
                    }
                }

                // rate limit 대기 — 인터럽트 시 깔끔하게 종료
                try {
                    Thread.sleep(CALL_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("[TMapRefresh] 인터럽트 수신 — 갱신 중단");
                    interrupted = true;
                    break;
                }
            }

            if (routes.isLast()) break;
            pageNumber++;
        }

        log.info("[TMapRefresh] 갱신 완료: 성공 {}, 실패 {}", updated, failed);
    }

    /**
     * 스케줄러 메서드 내부에서 @Transactional 메서드를 프록시 경유로 호출하기 위한 별도 컴포넌트.
     * 페이지 로드/단건 저장 각각을 짧은 트랜잭션으로 분리한다.
     */
    @Component
    @RequiredArgsConstructor
    static class TMapRouteUpdater {

        private final HubRouteRepository hubRouteRepository;

        @Transactional(readOnly = true)
        public Page<HubRoute> loadPage(Pageable pageable) {
            return hubRouteRepository.findAll(pageable);
        }

        @Transactional
        public void applyRefresh(UUID routeId, double distanceKm, double durationMin) {
            hubRouteRepository.findById(routeId)
                    .ifPresent(route -> route.refresh(distanceKm, durationMin));
        }
    }
}
