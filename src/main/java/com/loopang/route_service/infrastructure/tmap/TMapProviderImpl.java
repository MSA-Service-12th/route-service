package com.loopang.route_service.infrastructure.tmap;

import com.loopang.route_service.domain.service.TMapProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TMapProviderImpl implements TMapProvider {

    private static final String TMAP_API_URL = "https://apis.openapi.sk.com/tmap/routes?version=1";

    private final TMapProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public TMapRouteResult getRoute(double fromLat, double fromLon, double toLat, double toLon) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("appKey", properties.getApiKey());

        Map<String, Object> body = Map.of(
                "startX", String.valueOf(fromLon),
                "startY", String.valueOf(fromLat),
                "endX", String.valueOf(toLon),
                "endY", String.valueOf(toLat),
                "reqCoordType", "WGS84GEO",
                "resCoordType", "WGS84GEO",
                "searchOption", "0"
        );

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    TMAP_API_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            Map responseBody = response.getBody();
            if (responseBody == null) {
                log.error("[TMap] 응답 없음");
                return null;
            }

            Map features = (Map) ((java.util.List) responseBody.get("features")).get(0);
            Map prop = (Map) features.get("properties");

            int totalDistanceM = (int) prop.get("totalDistance");
            int totalTimeSec = (int) prop.get("totalTime");

            double distanceKm = totalDistanceM / 1000.0;
            double durationMin = totalTimeSec / 60.0;

            log.debug("[TMap] ({},{}) → ({},{}) = {:.1f}km, {:.1f}min",
                    fromLat, fromLon, toLat, toLon, distanceKm, durationMin);

            return new TMapRouteResult(
                    Math.round(distanceKm * 10) / 10.0,
                    Math.round(durationMin * 10) / 10.0
            );
        } catch (Exception e) {
            log.error("[TMap] API 호출 실패: ({},{}) → ({},{}): {}",
                    fromLat, fromLon, toLat, toLon, e.getMessage());
            return null;
        }
    }
}
