package se.david.microservices.core.order.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.david.api.core.order.dto.OrderUpdateDto;
import se.david.api.core.order.service.OrderService;
import se.david.api.event.Event;
import se.david.api.exceptions.EventProcessingException;
import se.david.microservices.core.order.domain.entity.Order;

import java.util.function.Consumer;


@Configuration
public class MessageProcessorConfig {
  private static final Logger LOG = LoggerFactory.getLogger(MessageProcessorConfig.class);
  private final OrderService orderService;

  @Autowired
  public MessageProcessorConfig(OrderService orderService) {
    this.orderService = orderService;

  }

  @Bean
  public Consumer<Event<Integer, Order>> messageProcessor() {
    return event -> {
      LOG.info("Process message created at {}...", event.getEventCreatedAt());

      switch(event.getEventType()) {
        case UPDATE:
          orderService.updateOrder(event.getKey(), new OrderUpdateDto(event.getData().getStatus()))
            .doOnSuccess(unused -> LOG.info("Successfully updated order for ID {}", event.getKey()))
            .doOnError(error -> LOG.error("Failed to update order for ID {}: {}", event.getKey(), error.getMessage()))
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
