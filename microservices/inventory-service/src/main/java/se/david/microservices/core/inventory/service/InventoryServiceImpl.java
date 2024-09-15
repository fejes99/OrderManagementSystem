package se.david.microservices.core.inventory.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
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
import java.util.stream.Collectors;

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
  public List<InventoryDto> getInventoryStocks() {
    List<Inventory> inventories = (List<Inventory>) repository.findAll();
    return inventories.stream()
      .map(this::mapToInventoryDtoWithServiceAddress)
      .collect(Collectors.toList());
  }

  private InventoryDto mapToInventoryDtoWithServiceAddress(Inventory inventory) {
    return new InventoryDto(inventory.getProductId(), inventory.getQuantity(), serviceUtil.getServiceAddress());
  }

  @Override
  public InventoryDto getInventoryStock(int productId) {
    LOG.debug("getInventoryStock: Search stock for productId: {}", productId);
    validateProductId(productId);
    Inventory inventory = findInventoryByProductId(productId);
    LOG.debug("getInventoryStock: Found stock for productId: {}", productId);
    return mapper.entityToDto(inventory);
  }

  private void validateProductId(int productId) {
    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }
  }

  private Inventory findInventoryByProductId(int productId) {
    Inventory inventory = repository.findByProductId(productId);
    if (inventory == null) {
      throw new NotFoundException("No product found for productId: " + productId);
    }
    return inventory;
  }


  @Override
  @Transactional
  public InventoryDto createInventoryStock(InventoryDto inventoryCreateRequest) {
    LOG.debug("createInventoryStock: Creating inventory for productId: {}", inventoryCreateRequest.productId());

    validateProductId(inventoryCreateRequest.productId());

    repository.findById(inventoryCreateRequest.productId())
      .ifPresent(inventory -> {
        throw new InvalidInputException("Inventory item already exists for productId: " + inventoryCreateRequest.productId());
      });

    Inventory inventory = mapper.dtoToEntity(inventoryCreateRequest);
    inventory = repository.save(inventory);

    LOG.debug("createInventoryStock: Successfully created inventory for productId: {}, quantity: {}",
      inventory.getProductId(), inventory.getQuantity());

    return mapper.entityToDto(inventory);
  }

  @Override
  @Transactional
  public void deleteInventoryStock(int productId) {
    LOG.debug("deleteInventoryStock: Deleting inventory for productId: {}", productId);

    validateProductId(productId);
    Inventory inventory = findInventoryByProductId(productId);

    repository.delete(inventory);

    LOG.debug("deleteInventoryStock: Successfully deleted inventory for productId: {}", productId);
  }


  @Override
  @Transactional
  public InventoryDto increaseStock(InventoryStockAdjustmentRequestDto inventoryIncreaseRequest) {
    validateStockAdjustmentRequest(inventoryIncreaseRequest);

    Inventory inventory = findInventoryByProductId(inventoryIncreaseRequest.productId());
    adjustStock(inventory, inventoryIncreaseRequest.quantity());
    LOG.debug("increaseStock: Increased stock for productId: {}, new quantity: {}",
      inventory.getProductId(), inventory.getQuantity());

    return mapper.entityToDto(inventory);
  }

  private void validateStockAdjustmentRequest(InventoryStockAdjustmentRequestDto request) {
    if (request.productId() < 1 || request.quantity() < 1) {
      throw new InvalidInputException("Invalid input: productId = " + request.productId() + ", quantity = " + request.quantity());
    }
  }

  private void adjustStock(Inventory inventory, int adjustmentQuantity) {
    inventory.setQuantity(inventory.getQuantity() + adjustmentQuantity);
    repository.save(inventory);
  }

  @Override
  @Transactional
  public void reduceStock(List<InventoryStockAdjustmentRequestDto> inventoryReduceRequests) {
    inventoryReduceRequests.forEach(this::processStockReduction);
  }

  private void processStockReduction(InventoryStockAdjustmentRequestDto reduceRequest) {
    validateStockAdjustmentRequest(reduceRequest);

    Inventory inventory = findInventoryByProductId(reduceRequest.productId());
    ensureSufficientStock(inventory, reduceRequest.quantity());
    adjustStock(inventory, -reduceRequest.quantity());

    LOG.debug("reduceStock: Reduced stock for productId: {}, reduced by: {}, new quantity: {}",
      inventory.getProductId(), reduceRequest.quantity(), inventory.getQuantity());
  }

  private void ensureSufficientStock(Inventory inventory, int quantityToReduce) {
    if (inventory.getQuantity() < quantityToReduce) {
      throw new InventoryOutOfStockException("Insufficient stock for productId: " + inventory.getProductId());
    }
  }
}
