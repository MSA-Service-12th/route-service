package com.loopang.route_service.domain.service;

/**
 * 두 지점 간 실제 도로 거리/시간을 계산하는 외부 라우팅 제공자.
 *
 * <p>구현체는 외부 API(Kakao Mobility, TMap, OSRM 등)나 로컬 알고리즘(Haversine 보정)을 사용한다.
 * 도메인 코드는 본 인터페이스만 의존하므로 제공자 교체가 자유롭다.</p>
 */
public interface RouteDistanceProvider {

    /**
     * 출발 → 도착 좌표 사이의 자동차 도로 거리/시간을 반환한다.
     *
     * @param fromLat 출발 위도
     * @param fromLon 출발 경도
     * @param toLat   도착 위도
     * @param toLon   도착 경도
     * @return 거리(km) + 시간(분). 호출 실패 시 {@code null} 또는 구현체별 폴백 값.
     */
    DistanceResult getDistance(double fromLat, double fromLon, double toLat, double toLon);

    record DistanceResult(double distanceKm, double durationMin) {}
}
