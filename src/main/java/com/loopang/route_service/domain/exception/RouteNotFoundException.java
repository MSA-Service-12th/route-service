package com.loopang.route_service.domain.exception;

import com.loopang.common.exception.NotFoundException;

import java.util.UUID;

public class RouteNotFoundException extends NotFoundException {

    public RouteNotFoundException(UUID id) {
        super("라우트를 찾을 수 없습니다. ID: " + id);
    }
}
