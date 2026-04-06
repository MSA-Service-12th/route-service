package com.loopang.route_service.presentation.dto.response;

import com.loopang.route_service.domain.vo.Direction;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * 업체 ↔ 허브 거리/시간 계산 응답.
 */
@Getter
@Builder
public class HubPointResponse {

    private UUID hubId;
    private Direction direction;
    private Double distance;     // km
    private Double duration;     // minutes
    private String provider;     // "KAKAO" 또는 "HAVERSINE_FALLBACK"

    public static HubPointResponse of(UUID hubId, Direction direction,
                                       double distance, double duration, String provider) {
        return HubPointResponse.builder()
                .hubId(hubId)
                .direction(direction)
                .distance(distance)
                .duration(duration)
                .provider(provider)
                .build();
    }
}
