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
        if (distance == null || distance < 0) throw new IllegalArgumentException("거리는 0 이상이어야 합니다.");
        if (duration == null || duration < 0) throw new IllegalArgumentException("시간은 0 이상이어야 합니다.");

        this.fromHubId = fromHubId;
        this.fromName = fromName;
        this.toHubId = toHubId;
        this.toName = toName;
        this.distance = distance;
        this.duration = duration;
        this.active = active != null ? active : (distance <= 150.0);
    }

    public void update(Double distance, Double duration, Boolean active) {
        if (distance != null) this.distance = distance;
        if (duration != null) this.duration = duration;
        if (active != null) this.active = active;
    }

    public void refresh(Double distance, Double duration) {
        this.distance = distance;
        this.duration = duration;
        this.active = distance <= 150.0;
        this.refreshedAt = LocalDateTime.now();
    }
}
