package se.david.microservices.core.shipping.domain.repository;

import org.springframework.data.repository.CrudRepository;
import se.david.microservices.core.shipping.domain.entity.Shipping;

import java.util.List;

public interface ShippingRepository extends CrudRepository<Shipping, Integer> {
  Shipping findByOrderId(Integer orderId);
  List<Shipping> findByOrderIdIn(List<Integer> orderId);
}
