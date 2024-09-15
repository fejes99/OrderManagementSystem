package se.david.microservices.core.inventory.domain.repository;

import org.springframework.data.repository.CrudRepository;
import se.david.microservices.core.inventory.domain.entity.Inventory;

public interface InventoryRepository extends CrudRepository<Inventory, Integer> {
}
