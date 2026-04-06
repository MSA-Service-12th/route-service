package com.loopang.route_service.domain.service;

import com.loopang.route_service.domain.service.dto.RouteCalculationResult;

import java.util.UUID;

public interface RouteCalculator {

    RouteCalculationResult calculate(UUID fromHubId, UUID toHubId);
}
