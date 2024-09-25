package se.david.microservices.core.product.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import se.david.api.core.inventory.dto.InventoryDto;
import se.david.api.core.product.dto.ProductCreateDto;
import se.david.api.core.product.dto.ProductDto;
import se.david.api.core.product.dto.ProductUpdateDto;
import se.david.api.core.product.service.ProductService;
import se.david.api.exceptions.InvalidInputException;
import se.david.api.exceptions.NotFoundException;
import se.david.microservices.core.product.domain.entity.Product;
import se.david.microservices.core.product.domain.repository.ProductRepository;
import se.david.microservices.core.product.mapper.ProductMapper;
import se.david.util.http.ServiceUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class ProductServiceImpl implements ProductService {
  private static final Logger LOG = LoggerFactory.getLogger(ProductServiceImpl.class);
  private final ProductRepository repository;
  private final ServiceUtil serviceUtil;
  private final ProductMapper mapper;

  @Autowired
  public ProductServiceImpl(ProductRepository repository, ServiceUtil serviceUtil, ProductMapper mapper) {
    this.repository = repository;
    this.serviceUtil = serviceUtil;
    this.mapper = mapper;
  }

  @Override
  public List<ProductDto> getProducts() {
    LOG.info("getProducts: Fetching all products");

    List<Product> products = (List<Product>) repository.findAll();
    return products.stream()
      .map(this::mapToProductDtoWithServiceAddress)
      .toList();
  }

  private ProductDto mapToProductDtoWithServiceAddress(Product product) {
    return new ProductDto(product.getId(), product.getName(), product.getDescription(), product.getPrice(), serviceUtil.getServiceAddress());
  }

  @Override
  public List<ProductDto> getProductsByIds(List<Integer> ids) {
    LOG.info("getProductsByIds: Fetching all products by list of ids");

    List<Product> products = repository.findByIdIn(ids);
    return products.stream()
      .map(this::mapToProductDtoWithServiceAddress)
      .collect(Collectors.toList());
  }

  @Override
  public ProductDto getProduct(int productId) {
    validateProductId(productId);
    Product product = repository.findById(productId)
      .orElseThrow(() -> new NotFoundException("Product with id " + productId + " not found"));
    return mapper.entityToDto(product);
  }

  private void validateProductId(int productId) {
    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }
  }

  @Override
  public ProductDto createProduct(ProductCreateDto productCreateDto) {
    LOG.debug("createProduct: Creating product for with name: {}", productCreateDto.name());

    Product product = mapper.createDtoToEntity(productCreateDto);
    product = repository.save(product);
    LOG.debug("createProduct: Successfully created product with name: {}, price: {}", product.getName(), product.getPrice());

    return mapper.entityToDto(product);
  }

  @Override
  public ProductDto updateProduct(int productId, ProductUpdateDto productUpdateDto) {
    validateProductId(productId);
    LOG.debug("updateProduct: Creating product for with id: {}", productId);

    Product product = repository.findById(productId)
      .orElseThrow(() -> new NotFoundException("Invalid productId: " + productId));

    mapper.updateEntityWithDto(product, productUpdateDto);
    Product updatedProduct = repository.save(product);

    LOG.debug("updateProduct: Successfully created product with id: {}", productId);
    return mapper.entityToDto(updatedProduct);
  }


  @Override
  public void deleteProduct(int productId) {
    LOG.debug("deleteProduct: Deleting product with id: {}", productId);

    validateProductId(productId);
    Product product = repository.findById(productId)
      .orElseThrow(() -> new NotFoundException("Product with id " + productId + " not found"));

    repository.delete(product);

    LOG.debug("deleteProduct: Successfully deleted product with id: {}", productId);
  }
}
