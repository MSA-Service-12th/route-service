package com.loopang.route_service.infrastructure.client;

import com.loopang.route_service.domain.service.dto.HubData;
import com.loopang.route_service.domain.service.dto.HubStatusData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "hub-service")
public interface HubFeignClient {

    @GetMapping("/api/hubs/{hubId}")
    HubStatusData getHub(@PathVariable("hubId") UUID hubId);

    @GetMapping("/api/hubs?size=50")
    HubListResponse getHubs();

    record HubListResponse(List<HubData> data) {}
}
