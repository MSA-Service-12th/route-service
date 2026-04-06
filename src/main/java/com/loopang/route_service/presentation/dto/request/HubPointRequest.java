package com.loopang.route_service.presentation.dto.request;

import com.loopang.route_service.domain.vo.Direction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 업체 ↔ 허브 거리/시간 계산 요청.
 *
 * <ul>
 *   <li>{@code hubId}: route-service가 hub-service Feign으로 좌표 조회</li>
 *   <li>{@code point}: 호출자(delivery-service)가 보유한 업체 좌표 (lat/lon)</li>
 *   <li>{@code direction}: TO_HUB(업체→허브) / FROM_HUB(허브→업체)</li>
 * </ul>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HubPointRequest {

    @NotNull(message = "hubId는 필수입니다.")
    private UUID hubId;

    @NotNull(message = "point는 필수입니다.")
    @Valid
    private GeoPoint point;

    @NotNull(message = "direction은 필수입니다. (TO_HUB 또는 FROM_HUB)")
    private Direction direction;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoPoint {

        @NotNull(message = "latitude는 필수입니다.")
        private Double latitude;

        @NotNull(message = "longitude는 필수입니다.")
        private Double longitude;
    }
}
