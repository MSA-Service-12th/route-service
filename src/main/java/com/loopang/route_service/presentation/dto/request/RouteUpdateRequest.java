package com.loopang.route_service.presentation.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RouteUpdateRequest {

    @PositiveOrZero(message = "거리는 0 이상이어야 합니다.")
    private Double distance;

    @PositiveOrZero(message = "소요 시간은 0 이상이어야 합니다.")
    private Double duration;

    private Boolean isActive;
}
