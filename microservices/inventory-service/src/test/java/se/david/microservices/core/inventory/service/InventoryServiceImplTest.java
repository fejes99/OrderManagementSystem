package se.david.microservices.core.inventory.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import se.david.api.core.inventory.dto.InventoryCreateDto;
import se.david.api.core.inventory.dto.InventoryStockAdjustmentRequestDto;
import se.david.api.exceptions.InvalidInputException;
import se.david.api.exceptions.InventoryOutOfStockException;
import se.david.api.exceptions.NotFoundException;
import se.david.microservices.core.inventory.domain.entity.Inventory;
import se.david.microservices.core.inventory.domain.repository.InventoryRepository;
import se.david.microservices.core.inventory.mapper.InventoryMapper;
import se.david.util.http.ServiceUtil;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

  @Mock
  private InventoryRepository repository;

  @Mock
  private ServiceUtil serviceUtil;

  @Mock
  private InventoryMapper mapper;

  private InventoryServiceImpl inventoryService;

  @BeforeEach
  void setUp() {
    inventoryService = new InventoryServiceImpl(repository, serviceUtil, mapper);
  }

  @Test
  @DisplayName("getInventoryStock returns the mapped DTO when the stock record exists")
  void getInventoryStockFoundMapsToDto() {
    Inventory inventory = new Inventory(1, 10);
    when(repository.findByProductId(1)).thenReturn(Mono.just(inventory));
    when(serviceUtil.getServiceAddress()).thenReturn("addr");

    StepVerifier.create(inventoryService.getInventoryStock(1))
      .expectNextMatches(dto -> dto.productId() == 1 && dto.quantity() == 10)
      .verifyComplete();
  }

  @Test
  @DisplayName("getInventoryStock emits NotFoundException when no stock record exists")
  void getInventoryStockNotFoundEmitsNotFoundException() {
    when(repository.findByProductId(1)).thenReturn(Mono.empty());

    StepVerifier.create(inventoryService.getInventoryStock(1))
      .expectError(NotFoundException.class)
      .verify();
  }

  @Test
  @DisplayName("getInventoryStock throws InvalidInputException for a non-positive productId")
  void getInventoryStockInvalidIdThrowsInvalidInputException() {
    assertThrows(InvalidInputException.class, () -> inventoryService.getInventoryStock(0));

    verifyNoInteractions(repository);
  }

  @Test
  @DisplayName("createInventoryStock emits InvalidInputException when a record already exists for the productId")
  void createInventoryStockAlreadyExistsEmitsInvalidInputException() {
    InventoryCreateDto createDto = new InventoryCreateDto(1, 5);
    when(repository.findByProductId(1)).thenReturn(Mono.just(new Inventory(1, 5)));

    StepVerifier.create(inventoryService.createInventoryStock(createDto))
      .expectError(InvalidInputException.class)
      .verify();

    verify(repository, never()).save(any());
  }

  @Test
  @DisplayName("createInventoryStock saves a new entity when none exists yet")
  void createInventoryStockNewSavesEntity() {
    InventoryCreateDto createDto = new InventoryCreateDto(2, 5);
    Inventory entity = new Inventory(2, 5);
    when(repository.findByProductId(2)).thenReturn(Mono.empty());
    when(mapper.createDtoToEntity(createDto)).thenReturn(entity);
    when(repository.save(entity)).thenReturn(Mono.just(entity));
    when(serviceUtil.getServiceAddress()).thenReturn("addr");

    StepVerifier.create(inventoryService.createInventoryStock(createDto))
      .expectNextMatches(dto -> dto.productId() == 2 && dto.quantity() == 5)
      .verifyComplete();
  }

  @Test
  @DisplayName("increaseStock adds the adjustment quantity to the existing quantity")
  void increaseStockAddsToExistingQuantity() {
    Inventory inventory = new Inventory(1, 10);
    InventoryStockAdjustmentRequestDto increaseDto = new InventoryStockAdjustmentRequestDto(1, 4);
    when(repository.findByProductId(1)).thenReturn(Mono.just(inventory));
    when(repository.save(inventory)).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(serviceUtil.getServiceAddress()).thenReturn("addr");

    StepVerifier.create(inventoryService.increaseStock(increaseDto))
      .expectNextMatches(dto -> dto.quantity() == 14)
      .verifyComplete();
  }

  @Test
  @DisplayName("reduceStocks subtracts the requested quantity from each item when stock is sufficient")
  void reduceStocksSufficientStockSubtractsFromEachItem() {
    Inventory inv1 = new Inventory(1, 10);
    Inventory inv2 = new Inventory(2, 5);
    when(repository.findByProductId(1)).thenReturn(Mono.just(inv1));
    when(repository.findByProductId(2)).thenReturn(Mono.just(inv2));
    when(repository.save(any(Inventory.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    List<InventoryStockAdjustmentRequestDto> requests = List.of(
      new InventoryStockAdjustmentRequestDto(1, 4),
      new InventoryStockAdjustmentRequestDto(2, 5));

    StepVerifier.create(inventoryService.reduceStocks(requests))
      .verifyComplete();

    assertEquals(6, inv1.getQuantity());
    assertEquals(0, inv2.getQuantity());
  }

  @Test
  @DisplayName("reduceStocks emits InventoryOutOfStockException and saves nothing when stock is insufficient")
  void reduceStocksInsufficientStockEmitsInventoryOutOfStockException() {
    Inventory inventory = new Inventory(1, 2);
    when(repository.findByProductId(1)).thenReturn(Mono.just(inventory));

    List<InventoryStockAdjustmentRequestDto> requests = List.of(new InventoryStockAdjustmentRequestDto(1, 5));

    StepVerifier.create(inventoryService.reduceStocks(requests))
      .expectError(InventoryOutOfStockException.class)
      .verify();

    verify(repository, never()).save(any());
  }

  @Test
  @DisplayName("deleteInventoryStock deletes the entity when it exists")
  void deleteInventoryStockFoundDeletesEntity() {
    Inventory inventory = new Inventory(1, 10);
    when(repository.findByProductId(1)).thenReturn(Mono.just(inventory));
    when(repository.delete(inventory)).thenReturn(Mono.empty());

    StepVerifier.create(inventoryService.deleteInventoryStock(1))
      .verifyComplete();

    verify(repository).delete(inventory);
  }
}
