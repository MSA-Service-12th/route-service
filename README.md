# Loopang Route Service

loopang MSA 프로젝트의 허브 간 경로 관리 서비스.
17개 허브 간 거리/시간을 TMap API로 계산하고, 다익스트라로 최단 경로를 제공한다.

## 기술 스택

- Java 21, Spring Boot 3.5.13, Spring Cloud 2025.0.1
- Spring MVC (Servlet 기반)
- 공통 모듈: `com.loopang:common:0.0.5-SNAPSHOT`
- PostgreSQL 17, JPA + QueryDSL
- TMap API (허브 간 실제 도로 거리/시간 계산)
- OpenFeign (hub-service 상태 조회)
- Eureka Client + Config Server
- Lombok, Gradle
- server.port = 18105

## 핵심 기능

1. **허브 라우트 CRUD** — 허브 간 직접 경로 관리 (from → to, 거리/시간)
2. **경로 계산 API** — 출발/도착 허브 → 다익스트라 최단 경로 (경유 허브 포함)
3. **TMap 스케줄러** — 1시간마다 17x17 허브 간 실제 도로 거리/시간 자동 갱신
4. **혼잡도 가중 경로** — 허브 상태(NORMAL/BUSY/FULL) 기반 경로 우회
5. **150km 초과 비활성화** — 직접 연결 거리 150km 초과 시 is_active = false

## 17개 허브

| 번호 | 이름 | 주소 |
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

## API 엔드포인트

### CRUD

| 메서드 | URL | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/hub-routes` | 라우트 등록 | MASTER |
| GET | `/api/hub-routes/{routeId}` | 라우트 단건 조회 | 전체 |
| GET | `/api/hub-routes` | 라우트 목록 조회 (페이징) | 전체 |
| PATCH | `/api/hub-routes/{routeId}` | 라우트 수정 | MASTER |
| DELETE | `/api/hub-routes/{routeId}` | 라우트 삭제 | MASTER |

### 경로 계산 (핵심)

| 메서드 | URL | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/hub-routes/calculate` | 최단 경로 계산 (다익스트라 + 혼잡도 가중) | 전체 |

### 경로 계산 요청

```json
POST /api/hub-routes/calculate
{
  "fromHubId": "00000000-0000-0000-0000-000000000001",
  "toHubId": "00000000-0000-0000-0000-000000000004"
}
```

### 경로 계산 응답

```json
{
  "data": {
    "fromHubId": "00000000-0000-0000-0000-000000000001",
    "toHubId": "00000000-0000-0000-0000-000000000004",
    "path": [
      { "sequence": 1, "hubId": "00000000-0000-0000-0000-000000000001" },
      { "sequence": 2, "hubId": "00000000-0000-0000-0000-000000000012" },
      { "sequence": 3, "hubId": "00000000-0000-0000-0000-000000000016" },
      { "sequence": 4, "hubId": "00000000-0000-0000-0000-000000000005" },
      { "sequence": 5, "hubId": "00000000-0000-0000-0000-000000000004" }
    ],
    "routeEdges": [
      { "sequence": 1, "fromHubId": "..001", "toHubId": "..012", "distance": 125.9, "duration": 111.8 },
      { "sequence": 2, "fromHubId": "..012", "toHubId": "..016", "distance": 144.5, "duration": 120.4 },
      { "sequence": 3, "fromHubId": "..016", "toHubId": "..005", "distance": 97.3, "duration": 84.8 },
      { "sequence": 4, "fromHubId": "..005", "toHubId": "..004", "distance": 113.0, "duration": 103.5 }
    ],
    "totalDistance": 480.7,
    "totalDuration": 420.5
  },
  "message": "허브 경로 계산에 성공했습니다."
}
```

## 경로 계산 알고리즘

### 다익스트라 + 혼잡도 가중

활성(is_active=true) 라우트만 사용하여 최단 경로를 계산한다.
경유 허브의 혼잡도에 따라 비용을 가중하여 혼잡한 허브를 자연스럽게 우회한다.

| 허브 상태 | 가중 계수 | 비용 계산 | 설명 |
|---|---|---|---|
| NORMAL (정상) | 0% | `cost = distance` | 가중 없음 |
| BUSY (혼잡) | 80% | `cost = distance x 1.8` | 거리 80% 가중 |
| FULL (만원) | 200% | `cost = distance x 3.0` | 거리 200% 가중 |

- 응답의 distance/duration은 **실제 값** (가중되지 않은 원본)
- 가중은 **경로 선택에만** 영향 (어떤 허브를 경유할지 결정)
- Hub Service Feign으로 실시간 허브 상태 조회
- Feign 실패 시 기본값 NORMAL로 처리

### TMap API 스케줄러

- 1시간마다 17x16=272개 허브 쌍의 실제 도로 거리/시간 갱신
- TMap API로 실제 도로 기반 거리(km), 시간(min) 계산
- 150km 초과 라우트는 is_active = false (직접 연결 비활성, 경유로만 도달)
- API 간 200ms 딜레이 (rate limit 대응)

### 초기 데이터

- 서버 시작 시 17x16=272개 라우트 자동 생성 (Haversine 직선거리)
- TMap 스케줄러가 실행되면 실제 도로 거리로 갱신

## 테이블: p_hub_route

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| route_id | UUID | PK | 라우트ID |
| from_hub_id | UUID | NOT NULL | 출발허브ID |
| from_name | VARCHAR(100) | NULL | 출발허브이름 |
| to_hub_id | UUID | NOT NULL | 도착허브ID |
| to_name | VARCHAR(100) | NULL | 도착허브이름 |
| distance | Double | NOT NULL | 거리 (km) |
| duration | Double | NOT NULL | 시간 (minutes) |
| is_active | BOOLEAN | NOT NULL | 활성화 (150km 이내) |
| refreshed_at | TIMESTAMP | NULL | TMap 갱신 시점 |
| version | INT | | 낙관적 락 |
| + BaseUserEntity |

## 다른 서비스 연동 방법

### 배송 서비스 (Delivery Service)

배송 생성 시 경로 계산을 요청합니다:

```text
1. Feign Client 정의:
   FeignClient(name = "route-service")
   PostMapping("/api/hub-routes/calculate")

2. 요청: { fromHubId: 공급업체허브, toHubId: 수령업체허브 }

3. 응답 활용:
   - path: 경유 허브 목록 → p_delivery_route의 sequence별 departure/destination
   - routeEdges: 구간별 거리/시간 → expected_distance, expected_time
   - totalDistance/Duration: 전체 배송 예상 거리/시간
```

### 주문 서비스 (Order Service)

주문 생성 시 예상 배송 시간 조회 (선택):

```text
Feign으로 /api/hub-routes/calculate 호출 → totalDuration으로 예상 소요 시간 제공
```

### 전체 배송 흐름

```text
업체A → (TMap: 업체→허브) → 출발허브
  → (다익스트라: 허브 간 최단 경로, 혼잡도 가중)
  → 경유허브1 → 경유허브2 → 도착허브
  → (TMap: 허브→업체) → 업체B
```

- 허브 간: Route Service (다익스트라 + TMap 데이터)
- 업체 ↔ 허브: Delivery Service에서 TMap 직접 호출

## 예외 처리

| 클래스 | HTTP | 설명 |
|---|---|---|
| RouteNotFoundException | 404 | 라우트를 찾을 수 없습니다 |
| RouteCalculationException | 404 | 경로를 찾을 수 없습니다 (도달 불가) |
| DuplicateRouteException | 409 | 이미 존재하는 라우트입니다 |

## 패키지 구조

```text
com.loopang.route_service
├── application/
│   └── RouteService.java
├── domain/
│   ├── entity/HubRoute.java
│   ├── repository/HubRouteRepository.java
│   ├── exception/
│   └── service/
│       ├── RouteCalculator.java
│       ├── TMapProvider.java
│       ├── HubStatusProvider.java
│       └── dto/HubStatusData.java
├── infrastructure/
│   ├── persistence/JpaHubRouteRepository.java
│   ├── route/DijkstraRouteCalculator.java
│   ├── tmap/TMapProviderImpl.java, TMapProperties.java
│   ├── client/HubFeignClient.java, HubStatusProviderImpl.java
│   ├── scheduler/TMapRefreshScheduler.java
│   └── init/HubRouteInitializer.java
└── presentation/
    ├── RouteController.java
    └── dto/request, response
```

## 로컬 실행

### 사전 조건
- Eureka Server (18761)
- Config Server (18888)
- PostgreSQL Docker (5435, DB: route)
- Hub Service (18100) — 혼잡도 가중 경로에 필요

### 환경변수 (IntelliJ Run Configuration)
```text
TMAP_API_KEY=본인TMap키;DB_URL=localhost:5435/route;DB_USERNAME=postgres;DB_PASSWORD=본인비밀번호
```

## 구현 현황

### 완료
- [x] HubRoute 엔티티 + CRUD + MASTER 권한 체크
- [x] 다익스트라 경로 계산 (is_active=true만)
- [x] 혼잡도 가중 경로 (Hub Service Feign 조회, NORMAL/BUSY/FULL)
- [x] TMap API 연동 (실제 도로 거리/시간 계산)
- [x] TMap 스케줄러 (1시간 갱신, 150km 초과 비활성화)
- [x] 초기 데이터 (17x16=272개 Haversine 라우트 자동 생성)
- [x] 예외 분리 (RouteNotFoundException, RouteCalculationException, DuplicateRouteException)

### TODO
- [ ] Kafka 연동 (Hub 변경 이벤트 수신 → 라우트 재계산)
- [ ] Redis 캐싱 (경로 계산 결과 캐싱, TTL 1시간)
- [ ] 검색 필터 (from_hub_id, to_hub_id, is_active — QueryDSL)
- [ ] SecurityUtil 전환
- [ ] Dockerfile + Docker 배포 + CI/CD
