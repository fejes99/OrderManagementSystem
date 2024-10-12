package se.david.microservices.core.inventory.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.david.api.core.inventory.dto.InventoryCreateDto;
import se.david.api.core.inventory.dto.InventoryDto;
import se.david.api.core.inventory.dto.InventoryStockAdjustmentRequestDto;
import se.david.api.core.inventory.service.InventoryService;
import se.david.api.exceptions.InvalidInputException;
import se.david.api.exceptions.InventoryOutOfStockException;
import se.david.api.exceptions.NotFoundException;
import se.david.microservices.core.inventory.domain.entity.Inventory;
import se.david.microservices.core.inventory.domain.repository.InventoryRepository;
import se.david.microservices.core.inventory.mapper.InventoryMapper;
import se.david.util.http.ServiceUtil;

import java.util.List;
import java.util.logging.Level;

@RestController
public class InventoryServiceImpl implements InventoryService {
  private static final Logger LOG = LoggerFactory.getLogger(InventoryServiceImpl.class);
  private final InventoryRepository repository;
  private final ServiceUtil serviceUtil;
  private final InventoryMapper mapper;

  @Autowired
  public InventoryServiceImpl(InventoryRepository repository, ServiceUtil serviceUtil, InventoryMapper mapper) {
    this.repository = repository;
    this.serviceUtil = serviceUtil;
    this.mapper = mapper;
  }

  @Override
  public Flux<InventoryDto> getInventoryStocks() {
    LOG.info("getInventoryStocks: Fetching all inventory stocks");
    return repository.findAll()
      .map(this::mapToInventoryDtoWithServiceAddress)
      .log(LOG.getName(), Level.FINE);
  }

  private InventoryDto mapToInventoryDtoWithServiceAddress(Inventory inventory) {
    return new InventoryDto(inventory.getProductId(), inventory.getQuantity(), serviceUtil.getServiceAddress());
  }

  @Override
  public Mono<InventoryDto> getInventoryStock(int productId) {
    LOG.debug("getInventoryStock: Search stock for productId: {}", productId);
    validateProductId(productId);
    return findInventoryByProductId(productId)
      .map(mapper::entityToDto)
      .log(LOG.getName(), Level.FINE);
  }

  private void validateProductId(int productId) {
    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }
  }

  private Mono<Inventory> findInventoryByProductId(int productId) {
    return repository.findByProductId(productId)
      .switchIfEmpty(Mono.error(new NotFoundException("No product found for productId: " + productId)))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  @Transactional
  public Mono<InventoryDto> createInventoryStock(InventoryCreateDto inventoryCreateDto) {
    LOG.debug("createInventoryStock: Creating inventory for productId: {}", inventoryCreateDto.productId());
    validateProductId(inventoryCreateDto.productId());

    return repository.findByProductId(inventoryCreateDto.productId())
      .flatMap(existing -> Mono.error(new InvalidInputException("Inventory item already exists for productId: " + inventoryCreateDto.productId())))
      .switchIfEmpty(Mono.defer(() -> {
        Inventory inventory = mapper.createDtoToEntity(inventoryCreateDto);
        return repository.save(inventory).map(mapper::entityToDto);
      }))
      .cast(InventoryDto.class)
      .log(LOG.getName(), Level.FINE);
  }


  @Override
  @Transactional
  public Mono<Void> deleteInventoryStock(int productId) {
    LOG.debug("deleteInventoryStock: Deleting inventory for productId: {}", productId);
    validateProductId(productId);
    return findInventoryByProductId(productId)
      .flatMap(inventory -> repository.delete(inventory).then(Mono.<Void>empty()))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  @Transactional
  public Mono<InventoryDto> increaseStock(InventoryStockAdjustmentRequestDto inventoryIncreaseDto) {
    validateStockAdjustmentRequest(inventoryIncreaseDto);

    return findInventoryByProductId(inventoryIncreaseDto.productId())
      .flatMap(inventory -> {
        adjustStock(inventory, inventoryIncreaseDto.quantity());
        return repository.save(inventory)
          .map(mapper::entityToDto);
      })
      .onErrorMap(DuplicateKeyException.class, ex ->
        new InvalidInputException("Duplicate key for productId: " + inventoryIncreaseDto.productId()))
      .log(LOG.getName(), Level.FINE);
  }

  private void validateStockAdjustmentRequest(InventoryStockAdjustmentRequestDto request) {
    if (request.productId() < 1 || request.quantity() < 1) {
      throw new InvalidInputException("Invalid input: productId = " + request.productId() + ", quantity = " + request.quantity());
    }
  }

  private void adjustStock(Inventory inventory, int adjustmentQuantity) {
    inventory.setQuantity(inventory.getQuantity() + adjustmentQuantity);
  }

  @Override
  @Transactional
  public Mono<Void> reduceStocks(List<InventoryStockAdjustmentRequestDto> inventoryReduceDtos) {
    return Flux.fromIterable(inventoryReduceDtos)
      .flatMap(this::processStockReduction)
      .then()
      .onErrorMap(DuplicateKeyException.class, ex ->
        new InvalidInputException("Duplicate key encountered during stock reduction"))
      .log(LOG.getName(), Level.FINE);
  }

  private Mono<Void> processStockReduction(InventoryStockAdjustmentRequestDto reduceRequest) {
    validateStockAdjustmentRequest(reduceRequest);

    return findInventoryByProductId(reduceRequest.productId())
      .flatMap(inventory -> {
        ensureSufficientStock(inventory, reduceRequest.quantity());
        adjustStock(inventory, -reduceRequest.quantity());
        return repository.save(inventory).then();
      })
      .onErrorMap(DuplicateKeyException.class, ex ->
        new InvalidInputException("Duplicate key for productId: " + reduceRequest.productId()))
      .log(LOG.getName(), Level.FINE);
  }

  private void ensureSufficientStock(Inventory inventory, int quantityToReduce) {
    if (inventory.getQuantity() < quantityToReduce) {
      throw new InventoryOutOfStockException("Insufficient stock for productId: " + inventory.getProductId());
    }
  }
}
