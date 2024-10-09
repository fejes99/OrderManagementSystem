package se.david.microservices.core.shipping.domain.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.david.microservices.core.shipping.domain.entity.Shipping;

import java.util.List;

public interface ShippingRepository extends ReactiveCrudRepository<Shipping, Integer> {
  Mono<Shipping> findByOrderId(Integer orderId);
  Flux<Shipping> findByOrderIdIn(List<Integer> orderId);
}
