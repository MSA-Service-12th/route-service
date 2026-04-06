package com.loopang.route_service.infrastructure.tmap;

import com.loopang.route_service.domain.service.TMapProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TMapProviderImpl implements TMapProvider {

    private static final String TMAP_API_URL = "https://apis.openapi.sk.com/tmap/routes?version=1";

    private final TMapProperties properties;
    private final RestTemplate restTemplate;

    public TMapProviderImpl(TMapProperties properties,
                            @Qualifier("tmapRestTemplate") RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

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
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(
                    TMAP_API_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            Map<?, ?> responseBody = response.getBody();
            if (responseBody == null) {
                log.error("[TMap] 응답 없음");
                return null;
            }

            List<?> features = (List<?>) responseBody.get("features");
            if (features == null || features.isEmpty()) {
                log.error("[TMap] features 비어있음");
                return null;
            }

            Map<?, ?> firstFeature = (Map<?, ?>) features.get(0);
            Map<?, ?> prop = (Map<?, ?>) firstFeature.get("properties");

            int totalDistanceM = ((Number) prop.get("totalDistance")).intValue();
            int totalTimeSec = ((Number) prop.get("totalTime")).intValue();

            double distanceKm = totalDistanceM / 1000.0;
            double durationMin = totalTimeSec / 60.0;

            log.debug("[TMap] ({},{}) → ({},{}) = {}km, {}min",
                    fromLat, fromLon, toLat, toLon, distanceKm, durationMin);

            return new TMapRouteResult(
                    Math.round(distanceKm * 10) / 10.0,
                    Math.round(durationMin * 10) / 10.0
            );
        } catch (RestClientException e) {
            log.error("[TMap] API 호출 실패: ({},{}) → ({},{}): {}",
                    fromLat, fromLon, toLat, toLon, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("[TMap] 응답 파싱 실패: ({},{}) → ({},{}): {}",
                    fromLat, fromLon, toLat, toLon, e.getMessage());
            return null;
        }
    }
}
