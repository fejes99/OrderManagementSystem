package se.david.microservices.core.shipping.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.david.api.core.shipping.dto.ShippingCreateDto;
import se.david.api.core.shipping.service.ShippingService;
import se.david.api.event.Event;
import se.david.api.exceptions.EventProcessingException;
import se.david.microservices.core.shipping.domain.entity.Shipping;
import se.david.microservices.core.shipping.mapper.ShippingMapper;

import java.util.function.Consumer;

@Configuration
public class MessageProcessorConfig {
  private static final Logger LOG = LoggerFactory.getLogger(MessageProcessorConfig.class);
  private final ShippingService shippingService;
  private final ShippingMapper shippingMapper;

  @Autowired
  public MessageProcessorConfig(ShippingService shippingService, ShippingMapper shippingMapper) {
    this.shippingService = shippingService;
    this.shippingMapper = shippingMapper;
  }


  @Bean
  public Consumer<Event<Integer, Shipping>> messageProcessor() {
    return event -> {
      LOG.info("Process message created at {}...", event.getEventCreatedAt());

      switch(event.getEventType()) {
        case CREATE:
          ShippingCreateDto createDto = shippingMapper.entityToCreateDto(event.getData());

          shippingService.createShippingOrder(createDto)
            .doOnSuccess(unused -> LOG.info("Successfully created shipping for order ID {}", event.getData().getOrderId()))
            .doOnError(error -> LOG.error("Failed to create shipping for order ID {}: {}", event.getData().getOrderId(), error.getMessage()))
            .subscribe();
          break;

        case UPDATE:
          shippingService.updateShippingStatusByOrderId(event.getData().getOrderId(), event.getData().getStatus())
            .doOnSuccess(unused -> LOG.info("Successfully updated shipping status for order ID {}", event.getData().getOrderId()))
            .doOnError(error -> LOG.error("Failed to update shipping status for order ID {}: {}", event.getData().getOrderId(), error.getMessage()))
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
