package com.loopang.route_service.domain.exception;

import com.loopang.common.exception.NotFoundException;

import java.util.UUID;

/**
 * hub-service Feign 호출 결과 허브를 찾지 못했거나 좌표가 비어 있을 때.
 */
public class HubLookupException extends NotFoundException {

    public HubLookupException(UUID hubId) {
        super("허브 정보를 조회할 수 없습니다. hubId=" + hubId);
    }
}
