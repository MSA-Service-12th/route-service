package com.loopang.route_service.domain.repository;

import com.loopang.route_service.domain.entity.HubRoute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HubRouteRepository {

    HubRoute save(HubRoute hubRoute);

    <S extends HubRoute> List<S> saveAll(Iterable<S> entities);

    Optional<HubRoute> findById(UUID id);

    Page<HubRoute> findAll(Pageable pageable);

    List<HubRoute> findAllByActiveTrue();

    Optional<HubRoute> findByFromHubIdAndToHubId(UUID fromHubId, UUID toHubId);

    boolean existsByFromHubIdAndToHubId(UUID fromHubId, UUID toHubId);

    long count();
}
