package com.loopang.route_service.domain.service;

import com.loopang.route_service.domain.service.dto.HubStatusData;

import java.util.UUID;

public interface HubStatusProvider {

    HubStatusData getHubStatus(UUID hubId);
}
