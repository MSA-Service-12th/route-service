package com.loopang.route_service.infrastructure.persistence;

import com.loopang.route_service.domain.entity.HubRoute;
import com.loopang.route_service.domain.repository.HubRouteRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaHubRouteRepository extends JpaRepository<HubRoute, UUID>, HubRouteRepository {
}
