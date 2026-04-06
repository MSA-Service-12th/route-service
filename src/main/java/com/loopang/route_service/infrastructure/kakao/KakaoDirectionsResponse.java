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
            Integer result_code,
            String result_msg,
            Summary summary
    ) {}

    /**
     * primitive 대신 {@code Integer}를 쓰는 이유:
     * Jackson은 primitive 필드가 JSON에 누락되면 자동으로 0으로 채운다.
     * 그러면 깨진 응답을 {@code distance=0, duration=0}인 정상 응답으로 오인할 수 있다.
     * Integer로 두면 누락 시 {@code null}이 되어 호출 측에서 명시적으로 검증할 수 있다.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Summary(
            Integer distance,    // meters
            Integer duration     // seconds
    ) {}
}
