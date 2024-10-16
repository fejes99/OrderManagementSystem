package se.david.microservices.core.product.domain.repository;

import org.springframework.data.repository.CrudRepository;
import se.david.microservices.core.product.domain.entity.Product;

import java.util.List;

public interface ProductRepository extends CrudRepository<Product, Integer> {
  List<Product> findByIdIn(List<Integer> ids);
}
