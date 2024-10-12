package se.david.microservices.core.inventory.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.david.api.core.inventory.dto.InventoryStockAdjustmentRequestDto;
import se.david.api.core.inventory.service.InventoryService;
import se.david.api.event.Event;
import se.david.api.exceptions.EventProcessingException;
import se.david.api.exceptions.InventoryOutOfStockException;
import se.david.microservices.core.inventory.domain.entity.Inventory;
import se.david.microservices.core.inventory.mapper.InventoryMapper;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Configuration
public class MessageProcessorConfig {
  private static final Logger LOG = LoggerFactory.getLogger(MessageProcessorConfig.class);
  private final InventoryService inventoryService;
  private final InventoryMapper inventoryMapper;

  @Autowired
  public MessageProcessorConfig(InventoryService inventoryService, InventoryMapper inventoryMapper) {
    this.inventoryService = inventoryService;
    this.inventoryMapper = inventoryMapper;
  }

  @Bean
  public Consumer<Event<Integer, Inventory>> messageProcessor() {
    return event -> {
      LOG.info("Process message created at {}...", event.getEventCreatedAt());

      switch(event.getEventType()) {
        case INCREASE_STOCK:
          InventoryStockAdjustmentRequestDto increaseRequest = inventoryMapper.entityToStockAdjustmentRequestDto(event.getData());

          inventoryService.increaseStock(increaseRequest)
            .doOnSuccess(unused -> LOG.info("Successfully increased stock for product ID {}", event.getKey()))
            .doOnError(error -> LOG.error("Failed to increase stock for product ID {}: {}", event.getKey(), error.getMessage()))
            .subscribe();
          break;

        case REDUCE_STOCKS:
          List<InventoryStockAdjustmentRequestDto> adjustmentRequests = event.getDataList().stream()
            .map(inventoryMapper::entityToStockAdjustmentRequestDto)
            .collect(Collectors.toList());

          inventoryService.reduceStocks(adjustmentRequests)
            .doOnSuccess(unused -> LOG.info("Successfully reduced stock for provided products."))
            .doOnError(error -> {
              if (error instanceof InventoryOutOfStockException) {
                LOG.warn("Stock reduction failed due to insufficient stock: {}", error.getMessage());
              } else {
                LOG.error("Failed to reduce stock: {}", error.getMessage());
              }
            })
            .subscribe();
          break;

        default:
          String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a CREATE or DELETE event";
          LOG.warn(errorMessage);
          throw new EventProcessingException(errorMessage);
      }
      LOG.info("Message processing done!");
    };
  }
}
