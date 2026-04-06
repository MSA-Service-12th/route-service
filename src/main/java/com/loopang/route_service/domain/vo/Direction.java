package com.loopang.route_service.domain.vo;

/**
 * 업체 ↔ 허브 구간의 방향.
 *
 * <p>실제 도로는 일방통행/우회 등으로 방향에 따라 거리가 다를 수 있어 명시적으로 받는다.</p>
 */
public enum Direction {
    /** 업체 → 허브 (첫 마일 / pickup) */
    TO_HUB,
    /** 허브 → 업체 (마지막 마일 / delivery) */
    FROM_HUB
}
