package se.david.microservices.core.order.domain.repository;

import org.springframework.data.repository.CrudRepository;
import se.david.microservices.core.order.domain.entity.OrderItem;

public interface OrderItemRepository extends CrudRepository<OrderItem, Integer> {
}
