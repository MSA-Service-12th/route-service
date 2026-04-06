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

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubRouteInitializer implements ApplicationRunner {

    private final HubRouteRepository hubRouteRepository;
    private final HubFeignClient hubFeignClient;

    private static final double MAX_ACTIVE_DISTANCE = 150.0;

    @Override
    public void run(ApplicationArguments args) {
        if (hubRouteRepository.findAllByActiveTrue().size() > 0) {
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
        int count = 0;

        for (int i = 0; i < hubs.size(); i++) {
            for (int j = 0; j < hubs.size(); j++) {
                if (i == j) continue;

                HubData from = hubs.get(i);
                HubData to = hubs.get(j);

                double distance = haversine(from.latitude(), from.longitude(), to.latitude(), to.longitude());
                double duration = distance / 60.0 * 60;

                HubRoute route = HubRoute.builder()
                        .fromHubId(from.hubId())
                        .fromName(from.name())
                        .toHubId(to.hubId())
                        .toName(to.name())
                        .distance(Math.round(distance * 10) / 10.0)
                        .duration(Math.round(duration * 10) / 10.0)
                        .active(distance <= MAX_ACTIVE_DISTANCE)
                        .build();

                hubRouteRepository.save(route);
                count++;
            }
        }

        log.info("[HubRouteInitializer] 초기화 완료: {}개 라우트 생성", count);
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
