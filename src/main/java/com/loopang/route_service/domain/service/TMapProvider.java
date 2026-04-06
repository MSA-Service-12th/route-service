package com.loopang.route_service.domain.service;

public interface TMapProvider {

    TMapRouteResult getRoute(double fromLat, double fromLon, double toLat, double toLon);

    record TMapRouteResult(double distanceKm, double durationMin) {}
}
