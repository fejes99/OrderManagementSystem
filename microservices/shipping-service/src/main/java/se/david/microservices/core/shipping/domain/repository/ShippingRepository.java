package se.david.microservices.core.shipping.domain.repository;

import org.springframework.data.repository.CrudRepository;
import se.david.microservices.core.shipping.domain.entity.Shipping;

public interface ShippingRepository extends CrudRepository<Shipping, Integer> {
}
