package com.loopang.route_service.application;

import com.loopang.route_service.domain.exception.HubLookupException;
import com.loopang.route_service.domain.service.HaversineCalculator;
import com.loopang.route_service.domain.service.RouteDistanceProvider;
import com.loopang.route_service.domain.service.RouteDistanceProvider.DistanceResult;
import com.loopang.route_service.domain.service.dto.HubData;
import com.loopang.route_service.domain.vo.Direction;
import com.loopang.route_service.infrastructure.client.HubFeignClient;
import com.loopang.route_service.presentation.dto.request.HubPointRequest;
import com.loopang.route_service.presentation.dto.response.HubPointResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 업체 ↔ 허브 거리/시간 계산.
 *
 * <ol>
 *   <li>hub-service Feign으로 허브 좌표 조회</li>
 *   <li>{@link Direction}에 따라 출발/도착 결정</li>
 *   <li>{@link RouteDistanceProvider}(Kakao Mobility)로 거리/시간 계산</li>
 *   <li>Kakao 호출 실패/null 시 Haversine 보정으로 폴백</li>
 * </ol>
 *
 * <p>본 서비스는 트랜잭션 없음 — 외부 호출만 수행한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HubPointService {

    private final HubFeignClient hubFeignClient;
    private final RouteDistanceProvider routeDistanceProvider;

    public HubPointResponse calculate(HubPointRequest request) {
        // 1. 허브 좌표 조회
        HubData hub = fetchHub(request.getHubId());
        double hubLat = hub.latitude();
        double hubLon = hub.longitude();

        double pointLat = request.getPoint().getLatitude();
        double pointLon = request.getPoint().getLongitude();

        // 2. direction에 따라 출발/도착 결정
        double fromLat, fromLon, toLat, toLon;
        if (request.getDirection() == Direction.TO_HUB) {
            // 업체 → 허브
            fromLat = pointLat;
            fromLon = pointLon;
            toLat = hubLat;
            toLon = hubLon;
        } else {
            // 허브 → 업체
            fromLat = hubLat;
            fromLon = hubLon;
            toLat = pointLat;
            toLon = pointLon;
        }

        // 3. Kakao 호출
        DistanceResult result = routeDistanceProvider.getDistance(fromLat, fromLon, toLat, toLon);

        if (result != null) {
            return HubPointResponse.of(
                    request.getHubId(),
                    request.getDirection(),
                    result.distanceKm(),
                    result.durationMin(),
                    "KAKAO"
            );
        }

        // 4. 폴백 — Haversine 보정
        log.warn("[HubPoint] Kakao 호출 실패 → Haversine 폴백 hubId={}", request.getHubId());
        double fallbackDistance = HaversineCalculator.estimatedRoadDistanceKm(fromLat, fromLon, toLat, toLon);
        double fallbackDuration = HaversineCalculator.estimatedDurationMin(fallbackDistance);

        return HubPointResponse.of(
                request.getHubId(),
                request.getDirection(),
                Math.round(fallbackDistance * 10) / 10.0,
                Math.round(fallbackDuration * 10) / 10.0,
                "HAVERSINE_FALLBACK"
        );
    }

    private HubData fetchHub(UUID hubId) {
        try {
            HubFeignClient.HubDetailResponse response = hubFeignClient.getHubDetail(hubId);
            HubData data = response != null ? response.data() : null;

            if (data == null
                    || data.hubId() == null
                    || data.latitude() == null
                    || data.longitude() == null) {
                throw new HubLookupException(hubId);
            }
            // 응답의 hubId가 요청 hubId와 일치하는지 (업스트림 오동작 방어)
            if (!hubId.equals(data.hubId())) {
                log.error("[HubPoint] 응답 hubId 불일치: requested={}, returned={}", hubId, data.hubId());
                throw new HubLookupException(hubId);
            }
            return data;
        } catch (HubLookupException e) {
            throw e;
        } catch (Exception e) {
            log.error("[HubPoint] hub-service 호출 실패 hubId={}: {}", hubId, e.getMessage());
            throw new HubLookupException(hubId);
        }
    }
}
