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
import org.springframework.dao.DataIntegrityViolationException;
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
        // 1차 방어: 동시 요청이 아니면 여기서 400에 가까운 친절한 에러로 끝낸다.
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

        try {
            // 2차 방어: 동시 요청일 때는 DB 유니크 제약(uk_hub_route_from_to)이 원자적으로 거른다.
            return RouteResponse.from(hubRouteRepository.save(route));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateRouteException();
        }
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
