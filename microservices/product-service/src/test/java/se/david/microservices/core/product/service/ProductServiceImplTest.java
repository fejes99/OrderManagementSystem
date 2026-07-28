package se.david.microservices.core.product.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import se.david.api.core.product.dto.ProductCreateDto;
import se.david.api.core.product.dto.ProductUpdateDto;
import se.david.api.exceptions.InvalidInputException;
import se.david.api.exceptions.NotFoundException;
import se.david.microservices.core.product.domain.entity.Product;
import se.david.microservices.core.product.domain.repository.ProductRepository;
import se.david.microservices.core.product.mapper.ProductMapper;
import se.david.util.http.ServiceUtil;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

  @Mock
  private ProductRepository repository;

  @Mock
  private ServiceUtil serviceUtil;

  @Mock
  private ProductMapper mapper;

  private ProductServiceImpl productService;

  @BeforeEach
  void setUp() {
    // subscribeOn(Schedulers.immediate()) keeps everything on the test thread so StepVerifier can
    // assert without needing to also coordinate across a background thread pool.
    productService = new ProductServiceImpl(Schedulers.immediate(), repository, serviceUtil, mapper);
  }

  @Test
  @DisplayName("getProduct returns the mapped DTO when the product exists")
  void getProductFoundMapsToDto() {
    Product product = new Product(1, "name", "desc", 100);
    when(repository.findById(1)).thenReturn(Optional.of(product));
    when(mapper.entityToDto(product)).thenReturn(new se.david.api.core.product.dto.ProductDto(1, "name", "desc", 100, "svc"));

    StepVerifier.create(productService.getProduct(1))
      .expectNextMatches(dto -> dto.id() == 1 && dto.name().equals("name"))
      .verifyComplete();
  }

  @Test
  @DisplayName("getProduct emits NotFoundException when the product does not exist")
  void getProductNotFoundEmitsNotFoundException() {
    when(repository.findById(1)).thenReturn(Optional.empty());

    StepVerifier.create(productService.getProduct(1))
      .expectError(NotFoundException.class)
      .verify();
  }

  @Test
  @DisplayName("getProduct throws InvalidInputException for a non-positive id")
  void getProductInvalidIdThrowsInvalidInputException() {
    // validateProductId() runs synchronously before the Mono is built, so the exception
    // surfaces immediately on the calling thread rather than as a reactive error signal.
    assertThrows(InvalidInputException.class, () -> productService.getProduct(0));

    verifyNoInteractions(repository);
  }

  @Test
  @DisplayName("getProducts maps every entity to a DTO carrying the service address")
  void getProductsMapsEachEntityWithServiceAddress() {
    // getProducts() builds ProductDto directly from the entity (not via mapper), so no mapper stub is needed.
    Product p1 = new Product(1, "a", "da", 10);
    Product p2 = new Product(2, "b", "db", 20);
    doReturn(List.of(p1, p2)).when(repository).findAll();
    when(serviceUtil.getServiceAddress()).thenReturn("addr");

    StepVerifier.create(productService.getProducts())
      .expectNextMatches(dto -> dto.id() == 1 && dto.serviceAddress().equals("addr"))
      .expectNextMatches(dto -> dto.id() == 2 && dto.serviceAddress().equals("addr"))
      .verifyComplete();
  }

  @Test
  @DisplayName("getProductsByIds delegates to repository.findByIdIn")
  void getProductsByIdsDelegatesToFindByIdIn() {
    Product p1 = new Product(5, "a", "da", 10);
    when(repository.findByIdIn(List.of(5))).thenReturn(List.of(p1));
    when(serviceUtil.getServiceAddress()).thenReturn("addr");

    StepVerifier.create(productService.getProductsByIds(List.of(5)))
      .expectNextMatches(dto -> dto.id() == 5)
      .verifyComplete();

    verify(repository).findByIdIn(List.of(5));
  }

  @Test
  @DisplayName("createProduct saves the mapped entity and returns its DTO")
  void createProductSavesMappedEntity() {
    ProductCreateDto createDto = new ProductCreateDto("name", "desc", 100);
    Product entity = new Product(0, "name", "desc", 100);
    Product saved = new Product(1, "name", "desc", 100);
    when(mapper.createDtoToEntity(createDto)).thenReturn(entity);
    when(repository.save(entity)).thenReturn(saved);
    when(mapper.entityToDto(saved)).thenReturn(new se.david.api.core.product.dto.ProductDto(1, "name", "desc", 100, "svc"));

    StepVerifier.create(productService.createProduct(createDto))
      .expectNextMatches(dto -> dto.id() == 1)
      .verifyComplete();
  }

  @Test
  @DisplayName("updateProduct emits NotFoundException and never saves when the product does not exist")
  void updateProductNotFoundEmitsNotFoundException() {
    when(repository.findById(99)).thenReturn(Optional.empty());

    StepVerifier.create(productService.updateProduct(99, new ProductUpdateDto("desc", 50)))
      .expectError(NotFoundException.class)
      .verify();

    verify(repository, never()).save(any());
  }

  @Test
  @DisplayName("deleteProduct deletes the entity when it exists")
  void deleteProductFoundDeletesEntity() {
    Product product = new Product(1, "name", "desc", 100);
    when(repository.findById(1)).thenReturn(Optional.of(product));

    StepVerifier.create(productService.deleteProduct(1))
      .verifyComplete();

    verify(repository).delete(product);
  }

  @Test
  @DisplayName("deleteProduct never touches the repository for a non-positive id")
  void deleteProductInvalidIdNeverTouchesRepository() {
    assertThrows(InvalidInputException.class, () -> productService.deleteProduct(-1));

    verifyNoInteractions(repository);
  }
}
