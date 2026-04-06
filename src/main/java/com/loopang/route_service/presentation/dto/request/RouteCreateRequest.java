package com.loopang.route_service.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RouteCreateRequest {

    @NotNull(message = "출발 허브 ID는 필수입니다.")
    private UUID fromHubId;

    @NotNull(message = "도착 허브 ID는 필수입니다.")
    private UUID toHubId;

    @NotNull(message = "거리는 필수입니다.")
    private Double distance;

    @NotNull(message = "소요 시간은 필수입니다.")
    private Double duration;

    private Boolean isActive;
}
