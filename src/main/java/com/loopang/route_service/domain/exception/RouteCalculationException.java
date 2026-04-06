package com.loopang.route_service.domain.exception;

import com.loopang.common.exception.NotFoundException;

public class RouteCalculationException extends NotFoundException {

    public RouteCalculationException() {
        super("경로를 찾을 수 없습니다.");
    }
}
