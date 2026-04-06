package com.loopang.route_service.infrastructure.kakao;

import com.loopang.route_service.domain.service.RouteDistanceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Kakao Mobility Directions API 기반 거리/시간 제공자.
 *
 * <p>요청:
 * <pre>
 * GET https://apis-navi.kakaomobility.com/v1/directions?origin=lng,lat&destination=lng,lat
 * Authorization: KakaoAK {REST_API_KEY}
 * </pre></p>
 *
 * <p>응답의 {@code summary.distance}(미터)와 {@code summary.duration}(초)을 km/분으로 변환해 반환.
 * 호출 실패 / 응답 이상 시 {@code null}을 반환하며, 호출자({@code HubPointService})가 폴백을 결정한다.</p>
 *
 * <p><b>주의:</b> Kakao API의 좌표 순서는 ({@code longitude}, {@code latitude})임에 유의.</p>
 */
@Slf4j
@Component
public class KakaoMobilityProviderImpl implements RouteDistanceProvider {

    private static final String DIRECTIONS_URL = "https://apis-navi.kakaomobility.com/v1/directions";

    private final RestTemplate restTemplate;
    private final KakaoProperties properties;

    public KakaoMobilityProviderImpl(@Qualifier("kakaoRestTemplate") RestTemplate restTemplate,
                                     KakaoProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public DistanceResult getDistance(double fromLat, double fromLon, double toLat, double toLon) {
        URI uri = UriComponentsBuilder.fromUriString(DIRECTIONS_URL)
                .queryParam("origin", fromLon + "," + fromLat)       // ← x,y (lng,lat)
                .queryParam("destination", toLon + "," + toLat)
                .build(true)
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.getRestApiKey());

        try {
            ResponseEntity<KakaoDirectionsResponse> response = restTemplate.exchange(
                    uri, HttpMethod.GET, new HttpEntity<>(headers), KakaoDirectionsResponse.class);

            KakaoDirectionsResponse body = response.getBody();
            if (body == null || body.routes() == null || body.routes().isEmpty()) {
                log.warn("[Kakao] 응답 비어있음: ({},{}) → ({},{})", fromLat, fromLon, toLat, toLon);
                return null;
            }

            KakaoDirectionsResponse.Route route = body.routes().get(0);
            if (route.result_code() != 0 || route.summary() == null) {
                log.warn("[Kakao] 길찾기 실패 result_code={} msg={}", route.result_code(), route.result_msg());
                return null;
            }

            double distanceKm = route.summary().distance() / 1000.0;
            double durationMin = route.summary().duration() / 60.0;

            log.debug("[Kakao] ({},{}) → ({},{}) = {}km, {}min",
                    fromLat, fromLon, toLat, toLon, distanceKm, durationMin);

            return new DistanceResult(
                    Math.round(distanceKm * 10) / 10.0,
                    Math.round(durationMin * 10) / 10.0
            );
        } catch (RestClientException e) {
            log.error("[Kakao] API 호출 실패: ({},{}) → ({},{}): {}",
                    fromLat, fromLon, toLat, toLon, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("[Kakao] 응답 파싱 실패: ({},{}) → ({},{}): {}",
                    fromLat, fromLon, toLat, toLon, e.getMessage());
            return null;
        }
    }
}
