package com.loopang.route_service.domain.service;

import com.loopang.route_service.presentation.dto.response.RouteCalculateResponse;

import java.util.UUID;

public interface RouteCalculator {

    RouteCalculateResponse calculate(UUID fromHubId, UUID toHubId);
}
