package com.loopang.route_service.infrastructure.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Kakao Mobility Directions API 응답.
 * <p>전체 응답 중 본 서비스가 사용하는 필드만 매핑한다 (route 단위 distance/duration).</p>
 *
 * <pre>
 * {
 *   "trans_id": "...",
 *   "routes": [
 *     {
 *       "result_code": 0,
 *       "result_msg": "길찾기 성공",
 *       "summary": {
 *         "distance": 384008,   // meters
 *         "duration": 16623     // seconds
 *       }
 *     }
 *   ]
 * }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoDirectionsResponse(
        List<Route> routes
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Route(
            int result_code,
            String result_msg,
            Summary summary
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Summary(
            int distance,    // meters
            int duration     // seconds
    ) {}
}
