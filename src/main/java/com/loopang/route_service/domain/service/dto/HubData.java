package com.loopang.route_service.domain.service.dto;

import java.util.UUID;

public record HubData(
        UUID hubId,
        String name,
        Double latitude,
        Double longitude
) {}
