package com.loopang.route_service.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class RouteDeleteResponse {

    private UUID routeId;
    private LocalDateTime deletedAt;
}
