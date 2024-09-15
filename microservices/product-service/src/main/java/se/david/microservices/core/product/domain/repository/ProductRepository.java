package se.david.microservices.core.product.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;
import se.david.microservices.core.product.domain.entity.Product;

import java.util.List;

public interface ProductRepository extends PagingAndSortingRepository<Product, Integer> {
  List<Product> findByIdIn(List<Integer> ids);
}
