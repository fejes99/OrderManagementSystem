package se.david.microservices.core.inventory.domain.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;
import se.david.microservices.core.inventory.domain.entity.Inventory;

public interface InventoryRepository extends CrudRepository<Inventory, Integer> {
  @Transactional(readOnly = true)
  Inventory findByProductId(int productId);
}
