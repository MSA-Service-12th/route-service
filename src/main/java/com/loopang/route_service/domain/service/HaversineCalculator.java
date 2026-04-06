package com.loopang.route_service.domain.service;

/**
 * Haversine 공식 기반 두 좌표 간 거리/시간 계산기.
 *
 * <p>외부 라우팅 API(Kakao, TMap)가 실패하거나 사용 불가일 때 폴백으로 사용한다.
 * 직선거리에 보정 계수({@value #ROAD_FACTOR})를 곱해 도로거리를 근사한다.
 * 한국 평균 시외 화물 기준 60km/h를 가정해 시간을 계산한다.</p>
 */
public final class HaversineCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    /** 직선거리 → 도로거리 근사 보정 계수 (한국 평균 약 1.3) */
    public static final double ROAD_FACTOR = 1.3;

    /** 평균 주행 속도 (km/h) — 시외 화물 기준 */
    public static final double AVG_SPEED_KMH = 60.0;

    private HaversineCalculator() {}

    /** 두 좌표 간 직선거리 (km). */
    public static double straightLineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /** 도로 거리 근사 (km) = 직선거리 × ROAD_FACTOR. */
    public static double estimatedRoadDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        return straightLineKm(lat1, lon1, lat2, lon2) * ROAD_FACTOR;
    }

    /** 예상 소요 시간 (분) = (거리 / 평균속도) × 60. */
    public static double estimatedDurationMin(double distanceKm) {
        return (distanceKm / AVG_SPEED_KMH) * 60;
    }
}
