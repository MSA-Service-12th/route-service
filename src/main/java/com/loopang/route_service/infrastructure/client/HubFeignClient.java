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

    /**
     * 허브 단건 상세 조회 — 좌표(latitude/longitude) 포함.
     * <p>응답 본문이 {@code {"data": {...}, "message": "..."}} 형식이라
     * 내부 record로 한 번 unwrap한다.</p>
     */
    @GetMapping("/api/hubs/{hubId}")
    HubDetailResponse getHubDetail(@PathVariable("hubId") UUID hubId);

    @GetMapping("/api/hubs?size=50")
    HubListResponse getHubs();

    record HubListResponse(List<HubData> data) {}

    record HubDetailResponse(HubData data) {}
}
