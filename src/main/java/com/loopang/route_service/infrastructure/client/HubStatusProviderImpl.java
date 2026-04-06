package com.loopang.route_service.infrastructure.client;

import com.loopang.route_service.domain.service.HubStatusProvider;
import com.loopang.route_service.domain.service.dto.HubStatusData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubStatusProviderImpl implements HubStatusProvider {

    private final HubFeignClient hubFeignClient;

    @Override
    public HubStatusData getHubStatus(UUID hubId) {
        try {
            return hubFeignClient.getHub(hubId);
        } catch (Exception e) {
            log.warn("[HubStatus] 허브 상태 조회 실패, 기본값(NORMAL) 사용: hubId={}", hubId);
            return new HubStatusData(hubId, "", "정상");
        }
    }
}
