package com.loopang.route_service.presentation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RouteUpdateRequest {

    private Double distance;
    private Double duration;
    private Boolean isActive;
}
