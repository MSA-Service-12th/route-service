package com.loopang.route_service.domain.exception;

import com.loopang.common.exception.ConflictException;

public class DuplicateRouteException extends ConflictException {

    public DuplicateRouteException() {
        super("이미 존재하는 라우트입니다.");
    }
}
