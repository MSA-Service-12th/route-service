package com.loopang.route_service.domain.service.dto;

import java.util.UUID;

public record HubStatusData(
        UUID hubId,
        String name,
        String status
) {}
