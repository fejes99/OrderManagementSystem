package se.david.microservices.composite.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.david.api.composite.order.dto.OrderAggregateCreateDto;
import se.david.api.composite.order.dto.OrderItemRequestDto;
import se.david.api.core.order.dto.OrderDto;
import se.david.api.core.order.dto.OrderItemDto;
import se.david.api.core.product.dto.ProductDto;
import se.david.api.core.shipping.dto.ShippingDto;
import se.david.microservices.composite.order.service.integration.OrderCompositeIntegration;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import({TestSecurityConfig.class, TestChannelBinderConfiguration.class})
@ActiveProfiles("test")
class OrderCompositeServiceApplicationTests {

  @Autowired
  private WebTestClient client;

  @MockBean
  private OrderCompositeIntegration integration;

  @DynamicPropertySource
  static void testProperties(DynamicPropertyRegistry registry) {
    registry.add("eureka.client.enabled", () -> "false");
    registry.add("spring.cloud.discovery.enabled", () -> "false");
    // Prevents ReactiveJwtDecoders.fromIssuerLocation() from making an eager network call to the
    // (unreachable, in tests) issuer-uri at context startup: a jwk-set-uri, even a dummy one,
    // takes priority and is only ever resolved lazily on first token decode.
    registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "http://localhost:0/jwks");
  }

  @Test
  @DisplayName("GET /order-composite/{orderId} aggregates order, shipping and product data from the (mocked) downstream services")
  void getCompositeOrderAggregatesDownstreamData() {
    int orderId = 1;
    OrderDto orderDto = new OrderDto(orderId, 10, 1900, "PENDING", new Date(),
      List.of(
        new OrderItemDto(1, orderId, 101, 2, 500),
        new OrderItemDto(2, orderId, 102, 3, 300)),
      "order-addr");
    ShippingDto shippingDto = new ShippingDto(orderId, "123 Main St", "Dispatched", "shipping-addr");
    List<ProductDto> products = List.of(
      new ProductDto(101, "Widget", "desc", 500, "product-addr"),
      new ProductDto(102, "Gadget", "desc", 300, "product-addr"));

    when(integration.getOrder(orderId)).thenReturn(Mono.just(orderDto));
    when(integration.getShippingByOrderId(orderId)).thenReturn(Mono.just(shippingDto));
    when(integration.getProductsByIds(List.of(101, 102))).thenReturn(Flux.fromIterable(products));

    client.get().uri("/order-composite/{orderId}", orderId)
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.orderId").isEqualTo(orderId)
      .jsonPath("$.totalPrice").isEqualTo(1900)
      .jsonPath("$.shippingSummary.status").isEqualTo("Dispatched")
      .jsonPath("$.orderItemsSummary.length()").isEqualTo(2);
  }

  @Test
  @DisplayName("GET /order-composite/{orderId} swallows a downstream failure (current onErrorResume behavior)")
  void getCompositeOrderDownstreamFailureIsSwallowed() {
    // Documents the current Phase 0/1 behavior: OrderCompositeServiceImpl.getCompositeOrder()
    // swallows downstream errors via onErrorResume(e -> Mono.empty()). Phase 3 (plan item 20)
    // replaces this with circuit-breaker/fallback semantics that let callers distinguish
    // "no data" from "a downstream call failed" - this test will need updating then.
    when(integration.getOrder(1)).thenReturn(Mono.error(new RuntimeException("boom")));

    client.get().uri("/order-composite/{orderId}", 1)
      .exchange()
      .expectBody().isEmpty();
  }

  @Test
  @DisplayName("POST /order-composite creates the order (with priced items) and its shipment via the downstream services")
  void createCompositeOrderCreatesOrderAndShipping() {
    OrderAggregateCreateDto createDto = new OrderAggregateCreateDto(10, "123 Main St",
      List.of(new OrderItemRequestDto(101, 2)));

    ProductDto product = new ProductDto(101, "Widget", "desc", 500, "product-addr");
    OrderDto createdOrder = new OrderDto(1, 10, 1000, "PENDING", new Date(), List.of(), "order-addr");
    ShippingDto createdShipping = new ShippingDto(1, "123 Main St", "Dispatched", "shipping-addr");

    when(integration.getProductsByIds(List.of(101))).thenReturn(Flux.just(product));
    when(integration.createOrder(any())).thenReturn(Mono.just(createdOrder));
    when(integration.createShippingOrder(any())).thenReturn(Mono.just(createdShipping));

    client.post().uri("/order-composite")
      .bodyValue(createDto)
      .exchange()
      .expectStatus().isOk();

    verify(integration).createOrder(argThat(order ->
      order.userId() == 10 && order.orderItems().get(0).price() == 500));
    verify(integration).createShippingOrder(argThat(shipping -> shipping.orderId() == 1));
  }
}
