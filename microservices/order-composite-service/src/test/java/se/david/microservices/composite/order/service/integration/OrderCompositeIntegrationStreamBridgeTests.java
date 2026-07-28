package se.david.microservices.composite.order.service.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Mono;
import se.david.api.core.inventory.dto.InventoryStockAdjustmentRequestDto;
import se.david.api.core.order.dto.OrderUpdateDto;
import se.david.api.core.shipping.dto.ShippingCreateDto;
import se.david.api.event.Event;
import se.david.microservices.composite.order.IsSameEvent;

import java.util.List;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestChannelBinderConfiguration.class)
@ActiveProfiles("test")
class OrderCompositeIntegrationStreamBridgeTests {

  @Autowired
  private OrderCompositeIntegration integration;

  @Autowired
  private OutputDestination outputDestination;

  @DynamicPropertySource
  static void testProperties(DynamicPropertyRegistry registry) {
    registry.add("eureka.client.enabled", () -> "false");
    registry.add("spring.cloud.discovery.enabled", () -> "false");
    registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "http://localhost:0/jwks");
  }

  @Test
  @DisplayName("createShippingOrder publishes a CREATE event to the shipments-out-0/shipments destination")
  void createShippingOrderPublishesCreateEventToShipmentsDestination() {
    ShippingCreateDto dto = new ShippingCreateDto(1, "123 Main St");
    Event<Integer, ShippingCreateDto> expectedEvent = new Event<>(Event.Type.CREATE, 1, dto);

    // The follow-up GET (fetching the created shipment back from shipping-service) fails here
    // because there's no real shipping-service to reach - that's fine: sendEvent() runs first in
    // the chain, so the message is already published by the time this settles either way.
    integration.createShippingOrder(dto).onErrorResume(e -> Mono.empty()).block();

    Message<byte[]> message = outputDestination.receive(2000, "shipments");
    assertThat(message, is(notNullValue()));
    assertThat(new String(Objects.requireNonNull(message).getPayload()),
      is(IsSameEvent.sameEventExceptCreatedAt(expectedEvent)));
  }

  @Test
  @DisplayName("updateOrder publishes an UPDATE event to the orders-out-0/orders destination")
  void updateOrderPublishesUpdateEventToOrdersDestination() {
    OrderUpdateDto dto = new OrderUpdateDto("SHIPPED");
    Event<Integer, OrderUpdateDto> expectedEvent = new Event<>(Event.Type.UPDATE, 5, dto);

    integration.updateOrder(5, dto).onErrorResume(e -> Mono.empty()).block();

    Message<byte[]> message = outputDestination.receive(2000, "orders");
    assertThat(message, is(notNullValue()));
    assertThat(new String(Objects.requireNonNull(message).getPayload()),
      is(IsSameEvent.sameEventExceptCreatedAt(expectedEvent)));
  }

  @Test
  @DisplayName("reduceStocks publishes a REDUCE_STOCKS event carrying a dataList to the inventories-out-0/inventories destination")
  void reduceStocksPublishesReduceStocksEventToInventoriesDestination() {
    // Regression test for a real bug this test originally caught: OrderCompositeIntegration used to
    // route reduceStocks() through the generic sendEvent(), whose <V> is a bare type variable at its
    // call site - javac could never see it as List<T> there, so it always built the Event via the
    // single-value Event(Type, K, T data) constructor even though the payload was a list. That put the
    // reductions under the JSON "data" field while inventory-service's consumer reads getDataList() for
    // REDUCE_STOCKS, which would have been null -> NullPointerException. reduceStocks() now goes through
    // the dedicated sendListEvent(), which the type system can actually prove matches Event(Type, K,
    // List<T> dataList) - asserting dataList (not data) carries the reductions here is the point of this test.
    List<InventoryStockAdjustmentRequestDto> reductions = List.of(new InventoryStockAdjustmentRequestDto(101, 2));
    Event<Integer, InventoryStockAdjustmentRequestDto> expectedEvent =
      new Event<>(Event.Type.REDUCE_STOCKS, null, reductions);

    integration.reduceStocks(reductions).block();

    Message<byte[]> message = outputDestination.receive(2000, "inventories");
    assertThat(message, is(notNullValue()));
    assertThat(new String(Objects.requireNonNull(message).getPayload()),
      is(IsSameEvent.sameEventExceptCreatedAt(expectedEvent)));
  }
}
