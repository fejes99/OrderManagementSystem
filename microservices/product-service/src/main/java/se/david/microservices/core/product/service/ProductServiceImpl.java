package se.david.microservices.core.product.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import se.david.api.core.product.dto.ProductDto;
import se.david.api.core.product.service.ProductService;
import se.david.util.http.ServiceUtil;

import java.util.List;

@RestController
public class ProductServiceImpl implements ProductService {
  private final ServiceUtil serviceUtil;

  @Autowired
  public ProductServiceImpl(ServiceUtil serviceUtil) {
    this.serviceUtil = serviceUtil;
  }

  @Override
  public List<ProductDto> getProducts() {
    return List.of();
  }

  @Override
  public ProductDto getProduct(int productId) {
    return null;
  }

  @Override
  public ProductDto createProduct(ProductDto product) {
    return null;
  }

  @Override
  public ProductDto updateProduct(int productId, ProductDto product) {
    return null;
  }

  @Override
  public void deleteProduct(int productId) {

  }
}
