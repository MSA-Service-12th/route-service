# Loopang Route Service

loopang MSA 프로젝트의 **허브 경로 + 업체↔허브 거리** 계산 서비스.
허브 ↔ 허브는 다익스트라(혼잡도 가중) + TMap, 업체 ↔ 허브는 Kakao Mobility로 분리해 처리한다.

---

## 🏷️ 기술 스택

| 분류 | 사용 기술 |
|---|---|
| Language / Framework | Java 21, Spring Boot 3.5.13, Spring Cloud 2025.0.1 |
| Web | Spring MVC (Servlet 기반) |
| 공통 모듈 | `com.loopang:common:0.0.5-SNAPSHOT` |
| DB / ORM | PostgreSQL 17, JPA, QueryDSL |
| 외부 라우팅 | TMap API (허브↔허브), Kakao Mobility Directions API (업체↔허브) |
| 서비스 간 통신 | Spring Cloud OpenFeign (hub-service) |
| 디스커버리 / 설정 | Eureka Client, Spring Cloud Config |
| 빌드 | Gradle |
| 포트 | `18105` |

---

## 🎯 핵심 기능

1. **허브 라우트 CRUD** — 허브 간 직접 경로 (from → to, 거리/시간) 관리
2. **허브↔허브 최단 경로** — 다익스트라 + 혼잡도 가중 (`POST /api/hub-routes/calculate`)
3. **업체↔허브 거리/시간** — Kakao Mobility (`POST /internal/hub-routes/hub-point`) ⭐
4. **TMap 스케줄러** — 1시간마다 17×16 허브 쌍 거리/시간 자동 갱신
5. **150km 초과 비활성화** — 직접 연결 거리 150km 초과 시 `is_active = false`

---

## 📡 API 엔드포인트

### 외부 — `/api/hub-routes/**`

| 메서드 | URL | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/hub-routes` | 라우트 등록 | MASTER |
| GET | `/api/hub-routes/{routeId}` | 라우트 단건 조회 | 전체 |
| GET | `/api/hub-routes` | 라우트 목록 (페이징) | 전체 |
| PATCH | `/api/hub-routes/{routeId}` | 라우트 수정 | MASTER |
| DELETE | `/api/hub-routes/{routeId}` | 라우트 삭제 | MASTER |
| POST | `/api/hub-routes/calculate` | **최단 경로 계산** (다익스트라 + 혼잡도) | 전체 |

### 내부 — `/internal/hub-routes/**` (delivery-service Feign 전용)

| 메서드 | URL | 설명 |
|---|---|---|
| POST | `/internal/hub-routes/hub-point` | **업체 ↔ 허브 거리/시간** (Kakao + Haversine 폴백) |

> ⚠️ **`/internal/**` 경로는 gateway 라우팅에서 제외되어 있습니다.** 서비스 간 Feign 호출은 Eureka 디스커버리로 route-service 인스턴스에 직접 도달. 외부 클라이언트는 gateway를 통해 접근할 수 없으나, route-service 포드/인스턴스가 네트워크에서 직접 노출되면 무인증 엔드포인트가 그대로 열리므로 ALB Listener Rule, Security Group, 또는 내부망 접근 제한 등 배포 시 네트워크 보안 설정이 적용된 환경을 전제로 한다.

---

## 🛣️ 허브 ↔ 허브 최단 경로 — `POST /api/hub-routes/calculate`

### 요청

```json
{
  "fromHubId": "00000000-0000-0000-0000-000000000001",
  "toHubId":   "00000000-0000-0000-0000-000000000004"
}
```

### 응답

```json
{
  "data": {
    "fromHubId": "...001",
    "toHubId":   "...004",
    "path": [
      { "sequence": 1, "hubId": "...001" },
      { "sequence": 2, "hubId": "...012" },
      { "sequence": 3, "hubId": "...016" },
      { "sequence": 4, "hubId": "...005" },
      { "sequence": 5, "hubId": "...004" }
    ],
    "routeEdges": [
      { "sequence": 1, "fromHubId": "...001", "toHubId": "...012", "distance": 125.9, "duration": 111.8 },
      { "sequence": 2, "fromHubId": "...012", "toHubId": "...016", "distance": 144.5, "duration": 120.4 },
      { "sequence": 3, "fromHubId": "...016", "toHubId": "...005", "distance":  97.3, "duration":  84.8 },
      { "sequence": 4, "fromHubId": "...005", "toHubId": "...004", "distance": 113.0, "duration": 103.5 }
    ],
    "totalDistance": 480.7,
    "totalDuration": 420.5
  },
  "message": "허브 경로 계산에 성공했습니다."
}
```

### 다익스트라 + 혼잡도 가중

활성(`is_active=true`) 라우트만 사용해 최단 경로를 계산. 경유 허브의 혼잡도에 따라 비용을 가중해 혼잡한 허브를 자연스럽게 우회한다.

| 허브 상태 | 가중 계수 | 비용 계산 |
|---|---|---|
| 정상 (NORMAL) | 0% | `distance × 1.0` |
| 혼잡 (BUSY) | 80% | `distance × 1.8` |
| 만원 (FULL) | 200% | `distance × 3.0` |

- 응답의 `distance`/`duration`은 **가중되지 않은 실제 값** — 가중은 경로 선택에만 영향
- `HubStatusProvider`(hub-service Feign) 실패 시 기본값 정상으로 처리

---

## 🚛 업체 ↔ 허브 거리/시간 — `POST /internal/hub-routes/hub-point` ⭐

### 요청

```json
{
  "hubId": "00000000-0000-0000-0000-000000000001",
  "point": { "latitude": 37.5163, "longitude": 127.0234 },
  "direction": "TO_HUB"
}
```

- `direction`: `TO_HUB` (업체→허브 / 첫 마일) 또는 `FROM_HUB` (허브→업체 / 마지막 마일)
- 실제 도로는 일방통행/우회가 있어 방향에 따라 거리가 다를 수 있음

### 응답

```json
{
  "data": {
    "hubId": "...001",
    "direction": "TO_HUB",
    "distance": 12.3,
    "duration": 25.0,
    "provider": "KAKAO"
  },
  "message": "허브-지점 거리 계산에 성공했습니다."
}
```

- `distance`: km
- `duration`: 분
- `provider`: `KAKAO`(정상) 또는 `HAVERSINE_FALLBACK`(Kakao 실패 시 자동 폴백)

### 흐름

```text
1. HubFeignClient.getHubDetail(hubId) — hub-service에서 좌표 조회
2. direction에 따라 origin/destination 결정 (Kakao API 좌표 순서: lng,lat)
3. KakaoMobilityProviderImpl.getDistance(...) — Kakao Mobility 호출
   ├─ result_code != 0 / null / 음수 / null 응답 → null 반환
   └─ 정상 → DistanceResult(km, min)
4. Kakao 실패 시 HaversineCalculator로 자동 폴백
   ├─ 직선거리 × 1.3 (도로거리 보정)
   └─ 60km/h 평균 속도로 시간 계산
```

### 책임 분리

- **호출자(delivery-service)는 좌표만 전달** — `companyId`는 받지 않음
- route-service가 company-service에 결합되지 않음 (도메인 경계 유지)
- 호출자는 자기가 보유한 업체 좌표를 그대로 넘기면 됨

---

## 🔌 외부 서비스 연동

### `HubFeignClient` → hub-service

| 메서드 | 호출 | 용도 |
|---|---|---|
| `getHub(hubId)` | `GET /api/hubs/{hubId}` | 혼잡도 가중 시 허브 상태 조회 |
| `getHubDetail(hubId)` | `GET /api/hubs/{hubId}` | hub-point 계산 시 좌표 조회 (lat/lon 포함) |
| `getHubs()` | `GET /api/hubs?size=50` | 17개 허브 일괄 조회 (TMap 스케줄러용) |

### `KakaoMobilityProviderImpl`

```text
GET https://apis-navi.kakaomobility.com/v1/directions
    ?origin={lng,lat}&destination={lng,lat}
Authorization: KakaoAK {REST_API_KEY}
```

응답의 `routes[0].summary.distance`(미터) / `duration`(초)을 km/분으로 변환.

**오류 처리:**

| 케이스 | 동작 |
|---|---|
| `RestClientException` (네트워크/타임아웃) | `null` 반환 → Haversine 폴백 |
| `result_code != 0` 또는 `summary == null` | `null` 반환 → 폴백 |
| `distance`/`duration` 누락(null) 또는 음수 | `null` 반환 → 폴백 |
| 정상 | `DistanceResult(km, min)` |

> Jackson 응답 매핑은 record + `Integer`(boxed) 사용 — 필드 누락 시 `0`으로 잘못 채워지는 것 방지.

### TMap (`TMapProviderImpl`)

- 1시간마다 17×16=272개 허브 쌍의 실제 도로 거리/시간 갱신
- API 간 200ms 딜레이 (rate limit 대응)
- 외부 I/O는 트랜잭션 밖, 저장만 단건 트랜잭션
- 150km 초과 라우트는 자동으로 `is_active = false`

---

## 🏗️ 데이터 모델

### `p_hub_route` — 허브 라우트

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| route_id | UUID | PK | |
| from_hub_id | UUID | NOT NULL | 출발 허브 ID |
| from_name | VARCHAR(100) | NULL | 출발 허브명 |
| to_hub_id | UUID | NOT NULL | 도착 허브 ID |
| to_name | VARCHAR(100) | NULL | 도착 허브명 |
| distance | Double | NOT NULL | 거리 (km) |
| duration | Double | NOT NULL | 시간 (분) |
| is_active | BOOLEAN | NOT NULL | 활성화 (150km 이내) |
| refreshed_at | TIMESTAMP | NULL | TMap 갱신 시점 |
| version | INT | | 낙관적 락 |
| + BaseUserEntity | | | createdAt/By, updatedAt/By, deletedAt/By |

### 17개 허브

| # | 이름 | 주소 |
|---|---|---|
| 1 | 서울특별시 센터 | 서울특별시 송파구 송파대로 55 |
| 2 | 경기 북부 센터 | 경기도 고양시 덕양구 권율대로 570 |
| 3 | 경기 남부 센터 | 경기도 이천시 덕평로 257-21 |
| 4 | 부산광역시 센터 | 부산 동구 중앙대로 206 |
| 5 | 대구광역시 센터 | 대구 북구 태평로 161 |
| 6 | 인천광역시 센터 | 인천 남동구 정각로 29 |
| 7 | 광주광역시 센터 | 광주 서구 내방로 111 |
| 8 | 대전광역시 센터 | 대전 서구 둔산로 100 |
| 9 | 울산광역시 센터 | 울산 남구 중앙로 201 |
| 10 | 세종특별자치시 센터 | 세종특별자치시 한누리대로 2130 |
| 11 | 강원특별자치도 센터 | 강원특별자치도 춘천시 중앙로 1 |
| 12 | 충청북도 센터 | 충북 청주시 상당구 상당로 82 |
| 13 | 충청남도 센터 | 충남 홍성군 홍북읍 충남대로 21 |
| 14 | 전북특별자치도 센터 | 전북특별자치도 전주시 완산구 효자로 225 |
| 15 | 전라남도 센터 | 전남 무안군 삼향읍 오룡길 1 |
| 16 | 경상북도 센터 | 경북 안동시 풍천면 도청대로 455 |
| 17 | 경상남도 센터 | 경남 창원시 의창구 중앙대로 300 |

---

## 🤝 다른 서비스와의 관계

### route-service가 호출하는 곳
- **hub-service** — `GET /api/hubs/{hubId}` (좌표 + 상태)
- **TMap API** — 허브↔허브 도로 거리 (스케줄러)
- **Kakao Mobility API** — 업체↔허브 도로 거리 (실시간)

### route-service를 호출하는 곳
- **delivery-service** (Feign)
  - `POST /api/hub-routes/calculate` — 허브 간 다익스트라 경로
  - `POST /internal/hub-routes/hub-point` — 업체↔허브 첫/마지막 마일
- **order-service** (선택) — 주문 생성 시 예상 시간 조회

### 전체 배송 흐름

```text
업체A
  ├─ /internal/hub-routes/hub-point { hubId: 출발허브, point: A, TO_HUB }
  ↓ Kakao
출발허브
  ├─ /api/hub-routes/calculate { fromHubId: 출발허브, toHubId: 도착허브 }
  ↓ 다익스트라 (TMap 데이터)
경유허브1 → 경유허브2 → 도착허브
  ├─ /internal/hub-routes/hub-point { hubId: 도착허브, point: B, FROM_HUB }
  ↓ Kakao
업체B
```

→ delivery-service는 위 3개 호출 결과를 조립해 `p_delivery_route`에 저장.

---

## 📑 예외 정책

| 클래스 | 상위 | HTTP | 설명 |
|---|---|---|---|
| `RouteNotFoundException` | NotFoundException | 404 | 라우트를 찾을 수 없습니다 |
| `RouteCalculationException` | NotFoundException | 404 | 경로를 찾을 수 없습니다 (도달 불가) |
| `DuplicateRouteException` | ConflictException | 409 | 이미 존재하는 라우트입니다 |
| `HubLookupException` | NotFoundException | 404 | 허브 좌표를 조회할 수 없습니다 |

전역 처리는 common의 `GlobalExceptionAdvice`가 담당.

---

## 🧱 패키지 구조

```text
com.loopang.route_service
├── application/
│   ├── RouteService.java                       # 허브 라우트 CRUD + 다익스트라 호출
│   └── HubPointService.java                    # 업체↔허브 (Kakao + Haversine 폴백)
├── domain/
│   ├── entity/HubRoute.java
│   ├── repository/HubRouteRepository.java
│   ├── exception/
│   │   ├── RouteNotFoundException.java
│   │   ├── RouteCalculationException.java
│   │   ├── DuplicateRouteException.java
│   │   └── HubLookupException.java
│   ├── service/
│   │   ├── RouteCalculator.java                # 다익스트라 인터페이스
│   │   ├── TMapProvider.java                   # TMap 추상화
│   │   ├── HubStatusProvider.java              # 혼잡도 조회 인터페이스
│   │   ├── RouteDistanceProvider.java          # ⭐ 외부 라우팅 추상화
│   │   ├── HaversineCalculator.java            # ⭐ Haversine 폴백 유틸
│   │   └── dto/
│   │       ├── HubData.java                    # hub 조회 응답 (lat/lon 포함)
│   │       ├── HubStatusData.java
│   │       └── RouteCalculationResult.java
│   └── vo/
│       └── Direction.java                      # ⭐ TO_HUB / FROM_HUB
├── infrastructure/
│   ├── persistence/JpaHubRouteRepository.java
│   ├── route/DijkstraRouteCalculator.java
│   ├── tmap/                                   # 허브↔허브용
│   │   ├── TMapProviderImpl.java
│   │   ├── TMapProperties.java
│   │   └── TMapRestTemplateConfig.java
│   ├── kakao/                                  # ⭐ 업체↔허브용
│   │   ├── KakaoMobilityProviderImpl.java
│   │   ├── KakaoDirectionsResponse.java
│   │   ├── KakaoProperties.java
│   │   └── KakaoRestTemplateConfig.java
│   ├── client/                                 # hub-service Feign
│   │   ├── HubFeignClient.java
│   │   └── HubStatusProviderImpl.java
│   ├── scheduler/TMapRefreshScheduler.java
│   └── init/HubRouteInitializer.java
└── presentation/
    ├── RouteController.java                    # /api/hub-routes/**
    ├── InternalHubPointController.java         # ⭐ /internal/hub-routes/hub-point
    └── dto/
        ├── request/
        │   ├── RouteCreateRequest.java
        │   ├── RouteUpdateRequest.java
        │   ├── RouteCalculateRequest.java
        │   └── HubPointRequest.java            # ⭐
        └── response/
            ├── RouteResponse.java
            ├── RouteDeleteResponse.java
            ├── RouteCalculateResponse.java
            └── HubPointResponse.java           # ⭐
```

**의존성 방향:** `presentation → application → domain ← infrastructure`

---

## 🏃 로컬 실행

### 사전 조건
- Eureka Server (`18761`)
- Config Server (`18888`)
- PostgreSQL Docker (`5435`, DB: `route`)
- **hub-service** (`18100`) — 좌표/상태 조회용

### 환경 변수 (IntelliJ Run Configuration)
```text
DB_URL=localhost:5435/route
DB_USERNAME=postgres
DB_PASSWORD=본인비밀번호
TMAP_API_KEY=본인_TMAP_키
KAKAO_REST_API_KEY=본인_카카오_REST_API_키
EUREKA_SERVER_URL=http://localhost:18761/eureka
```

### 통합 테스트 시나리오

```bash
# 1. hub-service에서 hubId 받기
curl http://localhost:18100/api/hubs?size=1

# 2. 허브 ↔ 허브 다익스트라 (서울→부산)
curl -X POST http://localhost:18105/api/hub-routes/calculate \
  -H "Content-Type: application/json" \
  -d '{ "fromHubId": "서울_HUB_ID", "toHubId": "부산_HUB_ID" }'

# 3. 업체 → 허브 (Kakao)
curl -X POST http://localhost:18105/internal/hub-routes/hub-point \
  -H "Content-Type: application/json" \
  -d '{
    "hubId": "서울_HUB_ID",
    "point": { "latitude": 37.5163, "longitude": 127.0234 },
    "direction": "TO_HUB"
  }'
```

기대 결과:
- ② 다익스트라 응답에 `path` + `routeEdges` + `totalDistance`/`totalDuration`
- ③ Kakao 응답에 `provider: "KAKAO"` (Kakao 키 정상 시) 또는 `provider: "HAVERSINE_FALLBACK"`

---

## 📋 구현 진행 상황

### ✅ 완료
- [x] HubRoute 엔티티 + CRUD + MASTER 권한
- [x] 다익스트라 경로 계산 (`is_active=true`만 사용)
- [x] 혼잡도 가중 경로 (Hub Service Feign 조회, 정상/혼잡/만원)
- [x] TMap API 연동 (스케줄러 1시간 갱신)
- [x] TMap 스케줄러 트랜잭션 분리, 페이지 순회, 인터럽트 복구
- [x] 150km 초과 자동 비활성화 (엔티티 불변식)
- [x] 초기 데이터 (17×16=272 Haversine 라우트 자동 생성)
- [x] 예외 분리 (`RouteNotFoundException`, `RouteCalculationException`, `DuplicateRouteException`)
- [x] 코드래빗 리뷰 다회 반영
- [x] **`POST /internal/hub-routes/hub-point` (Kakao Mobility) ⭐**
- [x] **`RouteDistanceProvider` 인터페이스 + Haversine 폴백**
- [x] **`HubLookupException` (404)**

### 🟡 다음
- [ ] `HubRouteInitializer`도 `HaversineCalculator` 사용하도록 정리 (인라인 메서드 제거)
- [ ] Redis 캐싱 (`(hubId, lat_round, lon_round, direction)` → 거리/시간) — 같은 업체 좌표 재호출 방지
- [ ] 검색 필터 (`from_hub_id`, `to_hub_id`, `is_active` — QueryDSL)

### 🔵 추후
- [ ] Kafka 연동 (Hub 변경 이벤트 수신 → 라우트 재계산)
- [ ] SecurityUtil 전환 (common Phase 4, 전 서비스 일괄)
- [ ] (Flyway 도입 시) `(from_hub_id, to_hub_id) WHERE deleted_at IS NULL` partial unique index
- [ ] `HubCongestionLevel` enum / 공유 DTO — 한글 문자열 매칭 → 타입 안전 전환
- [ ] Dockerfile + 배포 + GitHub Actions CI/CD
