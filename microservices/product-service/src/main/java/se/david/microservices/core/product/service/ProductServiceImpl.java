package se.david.microservices.core.product.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
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

import java.util.List;

@RestController
public class ProductServiceImpl implements ProductService {
  private static final Logger LOG = LoggerFactory.getLogger(ProductServiceImpl.class);
  private final ProductRepository repository;
  private final ServiceUtil serviceUtil;
  private final ProductMapper mapper;

  private final Scheduler jdbcScheduler;

  @Autowired
  public ProductServiceImpl(@Qualifier("jdbcScheduler") Scheduler jdbcScheduler, ProductRepository repository, ServiceUtil serviceUtil, ProductMapper mapper) {
    this.jdbcScheduler = jdbcScheduler;
    this.repository = repository;
    this.serviceUtil = serviceUtil;
    this.mapper = mapper;
  }

  @Override
  public Flux<ProductDto> getProducts() {
    LOG.info("Fetching all products");

    return Mono.fromCallable(this::internalGetProducts)
      .flatMapMany(Flux::fromIterable)
      .subscribeOn(jdbcScheduler)
      .map(this::mapToProductDtoWithServiceAddress)
      .doOnError(e -> LOG.error("Failed to fetch products", e));
  }

  private List<Product> internalGetProducts() {
    return (List<Product>) repository.findAll();
  }

  private ProductDto mapToProductDtoWithServiceAddress(Product product) {
    return new ProductDto(product.getId(), product.getName(), product.getDescription(), product.getPrice(), serviceUtil.getServiceAddress());
  }

  @Override
  public Flux<ProductDto> getProductsByIds(List<Integer> ids) {
    LOG.info("Fetching products for IDs: {}", ids);

    return Mono.fromCallable(() -> internalGetProductsByIds(ids))
      .flatMapMany(Flux::fromIterable)
      .subscribeOn(jdbcScheduler)
      .map(this::mapToProductDtoWithServiceAddress)
      .doOnError(e -> LOG.error("Failed to fetch products by IDs", e));
  }

  private List<Product> internalGetProductsByIds(List<Integer> ids) {
    return repository.findByIdIn(ids);
  }

  @Override
  public Mono<ProductDto> getProduct(int productId) {
    LOG.debug("Fetching product by ID: {}", productId);
    validateProductId(productId);

    return Mono.fromCallable(() -> findProductById(productId))
      .subscribeOn(jdbcScheduler)
      .map(mapper::entityToDto)
      .doOnError(e -> LOG.error("Failed to fetch product with ID: {}", productId, e));
  }

  private Product findProductById(int productId) {
    return repository.findById(productId)
      .orElseThrow(() -> new NotFoundException("Product with id " + productId + " not found"));
  }

  private void validateProductId(int productId) {
    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }
  }

  @Override
  public Mono<ProductDto> createProduct(ProductCreateDto productCreateDto) {
    LOG.debug("Creating product: {}", productCreateDto.name());

    return Mono.fromCallable(() -> internalCreateProduct(productCreateDto))
      .subscribeOn(jdbcScheduler)
      .map(mapper::entityToDto)
      .doOnSuccess(productDto -> LOG.debug("Created product with ID: {}", productDto.id()))
      .doOnError(e -> LOG.error("Failed to create product", e));
  }

  private Product internalCreateProduct(ProductCreateDto productCreateDto) {
    Product product = mapper.createDtoToEntity(productCreateDto);
    return repository.save(product);  // Blocking call
  }

  @Override
  public Mono<ProductDto> updateProduct(int productId, ProductUpdateDto productUpdateDto) {
    LOG.debug("Updating product with ID: {}", productId);
    validateProductId(productId);

    return Mono.fromCallable(() -> internalUpdateProduct(productId, productUpdateDto))
      .subscribeOn(jdbcScheduler)
      .map(mapper::entityToDto)
      .doOnSuccess(updatedProduct -> LOG.debug("Updated product with ID: {}", updatedProduct.id()))
      .doOnError(e -> LOG.error("Failed to update product with ID: {}", productId, e));
  }

  private Product internalUpdateProduct(int productId, ProductUpdateDto productUpdateDto) {
    Product product = repository.findById(productId)
      .orElseThrow(() -> new NotFoundException("Product with ID " + productId + " not found"));
    mapper.updateEntityWithDto(product, productUpdateDto);
    return repository.save(product);
  }

  @Override
  public Mono<Void> deleteProduct(int productId) {
    LOG.debug("Deleting product with ID: {}", productId);
    validateProductId(productId);

    return Mono.fromRunnable(() -> internalDeleteProduct(productId))
      .subscribeOn(jdbcScheduler)
      .doOnError(e -> LOG.error("Failed to delete product with ID: {}", productId, e))
      .then();
  }

  private void internalDeleteProduct(int productId) {
    Product product = repository.findById(productId)
      .orElseThrow(() -> new NotFoundException("Product with ID " + productId + " not found"));
    repository.delete(product);
  }
}
