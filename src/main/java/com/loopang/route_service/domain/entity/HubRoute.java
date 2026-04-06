package com.loopang.route_service.domain.entity;

import com.loopang.common.domain.BaseUserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_hub_route")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class HubRoute extends BaseUserEntity {

    public static final double MAX_ACTIVE_DISTANCE_KM = 150.0;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "route_id")
    private UUID id;

    @Version
    private int version;

    @Column(name = "from_hub_id", nullable = false)
    private UUID fromHubId;

    @Column(name = "from_name", length = 100)
    private String fromName;

    @Column(name = "to_hub_id", nullable = false)
    private UUID toHubId;

    @Column(name = "to_name", length = 100)
    private String toName;

    @Column(nullable = false)
    private Double distance;

    @Column(nullable = false)
    private Double duration;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "refreshed_at")
    private LocalDateTime refreshedAt;

    @Builder
    public HubRoute(UUID fromHubId, String fromName, UUID toHubId, String toName,
                    Double distance, Double duration, Boolean active) {
        if (fromHubId == null) throw new IllegalArgumentException("fromHubId는 필수입니다.");
        if (toHubId == null) throw new IllegalArgumentException("toHubId는 필수입니다.");
        if (fromHubId.equals(toHubId)) throw new IllegalArgumentException("출발 허브와 도착 허브는 같을 수 없습니다.");
        if (distance == null || distance < 0) throw new IllegalArgumentException("거리는 0 이상이어야 합니다.");
        if (duration == null || duration < 0) throw new IllegalArgumentException("시간은 0 이상이어야 합니다.");

        this.fromHubId = fromHubId;
        this.fromName = fromName;
        this.toHubId = toHubId;
        this.toName = toName;
        this.distance = distance;
        this.duration = duration;
        // 150km 초과는 무조건 비활성화 — 요청에서 true로 넘어와도 허용 불가
        boolean withinLimit = distance <= MAX_ACTIVE_DISTANCE_KM;
        this.active = withinLimit && (active == null || active);
    }

    public void update(Double distance, Double duration, Boolean active) {
        double nextDistance = distance != null ? distance : this.distance;
        double nextDuration = duration != null ? duration : this.duration;

        if (nextDistance < 0) throw new IllegalArgumentException("거리는 0 이상이어야 합니다.");
        if (nextDuration < 0) throw new IllegalArgumentException("시간은 0 이상이어야 합니다.");

        this.distance = nextDistance;
        this.duration = nextDuration;
        // 거리가 150km를 넘으면 항상 비활성. 그 외엔 명시된 active 값을 따르되, 미지정 시 기존 상태 유지.
        boolean withinLimit = nextDistance <= MAX_ACTIVE_DISTANCE_KM;
        boolean nextActive = active == null ? this.active : active;
        this.active = withinLimit && nextActive;
    }

    public void refresh(Double distance, Double duration) {
        if (distance == null || distance < 0) throw new IllegalArgumentException("거리는 0 이상이어야 합니다.");
        if (duration == null || duration < 0) throw new IllegalArgumentException("시간은 0 이상이어야 합니다.");

        this.distance = distance;
        this.duration = duration;
        this.active = distance <= MAX_ACTIVE_DISTANCE_KM;
        this.refreshedAt = LocalDateTime.now();
    }
}
