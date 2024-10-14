package se.david.microservices.core.inventory.domain.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import se.david.microservices.core.inventory.domain.entity.Inventory;

public interface InventoryRepository extends ReactiveCrudRepository<Inventory, Integer> {
  @Transactional(readOnly = true)
  Mono<Inventory> findByProductId(int productId);
}
