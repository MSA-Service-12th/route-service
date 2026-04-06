package com.loopang.route_service.presentation;

import com.loopang.common.response.CommonResponse;
import com.loopang.route_service.application.HubPointService;
import com.loopang.route_service.presentation.dto.request.HubPointRequest;
import com.loopang.route_service.presentation.dto.response.HubPointResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내부 서비스 호출 전용 — 업체 ↔ 허브 거리/시간 계산.
 *
 * <p>delivery-service 등 다른 MSA 컴포넌트가 Feign으로 호출한다.
 * 권한 검증이 없으며, 운영 환경에서는 ALB Listener Rule 또는 Security Group으로
 * {@code /internal/**}을 외부에서 직접 호출하는 트래픽을 차단해야 한다.</p>
 *
 * <p>외부 클라이언트가 gateway를 통해 접근할 수 없도록, gateway 라우팅에는
 * {@code /internal/hub-routes/**} 경로를 등록하지 않는다.</p>
 */
@RestController
@RequestMapping("/internal/hub-routes")
@RequiredArgsConstructor
public class InternalHubPointController {

    private final HubPointService hubPointService;

    /**
     * 업체 좌표(point) ↔ 허브(hubId) 사이의 자동차 도로 거리/시간 계산.
     * <p>제공자: Kakao Mobility Directions API.
     * 호출 실패 시 Haversine 보정으로 자동 폴백.</p>
     */
    @PostMapping("/hub-point")
    public CommonResponse<HubPointResponse> calculate(@Valid @RequestBody HubPointRequest request) {
        return CommonResponse.success(
                hubPointService.calculate(request),
                "허브-지점 거리 계산에 성공했습니다."
        );
    }
}
