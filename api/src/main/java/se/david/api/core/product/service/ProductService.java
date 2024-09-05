package se.david.api.core.product.service;

import org.springframework.web.bind.annotation.*;
import se.david.api.core.product.dto.ProductDto;

import java.util.List;

public interface ProductService {
  @GetMapping(
    value = "/products",
    produces = "application/json")
  List<ProductDto> getProducts();

  @GetMapping(
    value = "/products/{productId}",
    produces = "application/json")
  ProductDto getProduct(@PathVariable int productId);

  @PostMapping(
    value = "/products",
    consumes = "application/json",
    produces = "application/json")
  ProductDto createProduct(@RequestBody ProductDto product);

  @PutMapping(
    value = "/products/{productId}",
    consumes = "application/json",
    produces = "application/json")
  ProductDto updateProduct(@PathVariable int productId, @RequestBody ProductDto product);

  @DeleteMapping(
    value = "/products/{productId}")
  void deleteProduct(@PathVariable int productId);
}
