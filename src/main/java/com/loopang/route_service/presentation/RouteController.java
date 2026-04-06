package com.loopang.route_service.presentation;

import com.loopang.common.exception.ForbiddenException;
import com.loopang.common.response.CommonResponse;
import com.loopang.common.response.PageInfo;
import com.loopang.route_service.application.RouteService;
import com.loopang.route_service.presentation.dto.request.RouteCalculateRequest;
import com.loopang.route_service.presentation.dto.request.RouteCreateRequest;
import com.loopang.route_service.presentation.dto.request.RouteUpdateRequest;
import com.loopang.route_service.presentation.dto.response.RouteCalculateResponse;
import com.loopang.route_service.presentation.dto.response.RouteDeleteResponse;
import com.loopang.route_service.presentation.dto.response.RouteResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hub-routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<RouteResponse> create(
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody RouteCreateRequest request) {
        checkMaster(userRole);
        return CommonResponse.success(routeService.create(request), "허브 라우트 등록에 성공했습니다.");
    }

    @GetMapping("/{routeId}")
    public CommonResponse<RouteResponse> getRoute(@PathVariable UUID routeId) {
        return CommonResponse.success(routeService.getRoute(routeId), "허브 라우트 조회에 성공했습니다.");
    }

    @GetMapping
    public CommonResponse<List<RouteResponse>> getRoutes(Pageable pageable) {
        Page<RouteResponse> page = routeService.getRoutes(pageable);
        return CommonResponse.success(page.getContent(), "허브 라우트 목록 조회에 성공했습니다.", PageInfo.from(page));
    }

    @PatchMapping("/{routeId}")
    public CommonResponse<RouteResponse> updateRoute(
            @PathVariable UUID routeId,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody RouteUpdateRequest request) {
        checkMaster(userRole);
        return CommonResponse.success(routeService.updateRoute(routeId, request), "허브 라우트 수정에 성공했습니다.");
    }

    @DeleteMapping("/{routeId}")
    public CommonResponse<RouteDeleteResponse> deleteRoute(
            @PathVariable UUID routeId,
            @RequestHeader("X-User-Role") String userRole) {
        checkMaster(userRole);
        return CommonResponse.success(routeService.deleteRoute(routeId), "허브 라우트 삭제에 성공했습니다.");
    }

    @PostMapping("/calculate")
    public CommonResponse<RouteCalculateResponse> calculate(
            @Valid @RequestBody RouteCalculateRequest request) {
        return CommonResponse.success(routeService.calculate(request), "허브 경로 계산에 성공했습니다.");
    }

    private void checkMaster(String userRole) {
        if (!"ROLE_MASTER".equals(userRole)) {
            throw new ForbiddenException("마스터 관리자만 수행할 수 있는 작업입니다.");
        }
    }
}
