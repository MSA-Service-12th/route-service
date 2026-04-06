package com.loopang.route_service.infrastructure.init;

import com.loopang.route_service.domain.entity.HubRoute;
import com.loopang.route_service.domain.repository.HubRouteRepository;
import com.loopang.route_service.domain.service.dto.HubData;
import com.loopang.route_service.infrastructure.client.HubFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubRouteInitializer implements ApplicationRunner {

    private final HubRouteRepository hubRouteRepository;
    private final HubFeignClient hubFeignClient;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // active 여부가 아니라 "라우트가 하나라도 있는지"로 판단.
        // TMap 갱신으로 전부 비활성화돼도 재생성되지 않도록.
        if (hubRouteRepository.count() > 0) {
            log.info("[HubRouteInitializer] 이미 초기 데이터 존재 — 스킵");
            return;
        }

        List<HubData> hubs;
        try {
            HubFeignClient.HubListResponse response = hubFeignClient.getHubs();
            hubs = response.data();
        } catch (Exception e) {
            log.warn("[HubRouteInitializer] Hub Service 연결 실패 — 초기 데이터 생성 스킵. 나중에 Hub Service 시작 후 재시작 필요.");
            return;
        }

        if (hubs == null || hubs.isEmpty()) {
            log.warn("[HubRouteInitializer] 허브 데이터 없음 — 스킵");
            return;
        }

        log.info("[HubRouteInitializer] {}개 허브 간 라우트 초기화 시작", hubs.size());

        List<HubRoute> routes = new ArrayList<>(hubs.size() * (hubs.size() - 1));
        int skipped = 0;
        for (int i = 0; i < hubs.size(); i++) {
            for (int j = 0; j < hubs.size(); j++) {
                if (i == j) continue;

                HubData from = hubs.get(i);
                HubData to = hubs.get(j);

                // Hub Service 응답에 null 좌표/ID가 섞여 있어도 전체 초기화가 NPE로 죽지 않도록 방어.
                if (!hasValidCoordinates(from) || !hasValidCoordinates(to)) {
                    log.warn("[HubRouteInitializer] 좌표/ID 누락 허브 스킵: {} → {}",
                            from == null ? "null" : from.name(),
                            to == null ? "null" : to.name());
                    skipped++;
                    continue;
                }

                double distance = haversine(from.latitude(), from.longitude(), to.latitude(), to.longitude());
                // 60km/h 평균 속도 가정 → distance(km) / 60 * 60(min)
                double duration = distance;

                routes.add(HubRoute.builder()
                        .fromHubId(from.hubId())
                        .fromName(from.name())
                        .toHubId(to.hubId())
                        .toName(to.name())
                        .distance(Math.round(distance * 10) / 10.0)
                        .duration(Math.round(duration * 10) / 10.0)
                        // active는 HubRoute 생성자에서 150km 규칙으로 자동 결정
                        .build());
            }
        }

        if (skipped > 0) {
            log.warn("[HubRouteInitializer] 좌표 누락으로 {}쌍 스킵됨", skipped);
        }

        // 한 트랜잭션에 한 번의 저장 — 중간 실패 시 전부 롤백돼 부분 초기화가 고착되지 않는다.
        hubRouteRepository.saveAll(routes);
        log.info("[HubRouteInitializer] 초기화 완료: {}개 라우트 생성", routes.size());
    }

    private boolean hasValidCoordinates(HubData hub) {
        return hub != null
                && hub.hubId() != null
                && hub.latitude() != null
                && hub.longitude() != null;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
