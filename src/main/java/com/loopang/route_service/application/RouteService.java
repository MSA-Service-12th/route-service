package com.loopang.route_service.application;

import com.loopang.route_service.domain.entity.HubRoute;
import com.loopang.route_service.domain.exception.DuplicateRouteException;
import com.loopang.route_service.domain.exception.RouteNotFoundException;
import com.loopang.route_service.domain.repository.HubRouteRepository;
import com.loopang.route_service.domain.service.RouteCalculator;
import com.loopang.route_service.domain.service.dto.RouteCalculationResult;
import com.loopang.route_service.presentation.dto.request.RouteCalculateRequest;
import com.loopang.route_service.presentation.dto.request.RouteCreateRequest;
import com.loopang.route_service.presentation.dto.request.RouteUpdateRequest;
import com.loopang.route_service.presentation.dto.response.RouteCalculateResponse;
import com.loopang.route_service.presentation.dto.response.RouteDeleteResponse;
import com.loopang.route_service.presentation.dto.response.RouteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouteService {

    private final HubRouteRepository hubRouteRepository;
    private final RouteCalculator routeCalculator;

    @Transactional
    public RouteResponse create(RouteCreateRequest request) {
        // @SQLRestriction("deleted_at IS NULL")을 거쳐 "살아 있는" 라우트만 확인한다.
        // DB 유니크 제약을 걸면 soft-delete된 라우트와 충돌해 (from, to) 재등록이 막히므로 제약은 두지 않는다.
        // 이 검사는 MASTER 수동 등록 가정 — 동시 요청이 사실상 없는 환경.
        // TODO: partial unique index(Flyway 도입 시) 또는 복구 플로우로 race condition 방어 강화.
        if (hubRouteRepository.existsByFromHubIdAndToHubId(request.getFromHubId(), request.getToHubId())) {
            throw new DuplicateRouteException();
        }

        HubRoute route = HubRoute.builder()
                .fromHubId(request.getFromHubId())
                .toHubId(request.getToHubId())
                .distance(request.getDistance())
                .duration(request.getDuration())
                .active(request.getIsActive())
                .build();

        return RouteResponse.from(hubRouteRepository.save(route));
    }

    public RouteResponse getRoute(UUID routeId) {
        return RouteResponse.from(findById(routeId));
    }

    public Page<RouteResponse> getRoutes(Pageable pageable) {
        return hubRouteRepository.findAll(pageable).map(RouteResponse::from);
    }

    @Transactional
    public RouteResponse updateRoute(UUID routeId, RouteUpdateRequest request) {
        HubRoute route = findById(routeId);
        route.update(request.getDistance(), request.getDuration(), request.getIsActive());
        return RouteResponse.from(route);
    }

    @Transactional
    public RouteDeleteResponse deleteRoute(UUID routeId) {
        HubRoute route = findById(routeId);
        route.delete(null); // TODO: SecurityUtil 전환 후 현재 유저 UUID 전달
        return RouteDeleteResponse.builder()
                .routeId(routeId)
                .deletedAt(route.getDeletedAt()) // BaseUserEntity.delete()가 세팅한 값을 그대로 사용 — 응답과 DB 값 일치 보장
                .build();
    }

    public RouteCalculateResponse calculate(RouteCalculateRequest request) {
        RouteCalculationResult result = routeCalculator.calculate(request.getFromHubId(), request.getToHubId());
        return RouteCalculateResponse.from(result);
    }

    private HubRoute findById(UUID routeId) {
        return hubRouteRepository.findById(routeId)
                .orElseThrow(() -> new RouteNotFoundException(routeId));
    }
}
