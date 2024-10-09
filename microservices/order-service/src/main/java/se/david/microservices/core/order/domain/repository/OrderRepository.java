package se.david.microservices.core.order.domain.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import se.david.microservices.core.order.domain.entity.Order;

import java.util.List;

public interface OrderRepository extends CrudRepository<Order, Integer> {
  List<Order> findByUserId(Integer userId);
}
