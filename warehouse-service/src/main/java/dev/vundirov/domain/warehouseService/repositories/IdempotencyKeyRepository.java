package dev.vundirov.domain.warehouseService.repositories;

import dev.vundirov.domain.warehouseService.entities.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {
}