package se.david.microservices.core.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.david.api.core.order.dto.OrderCreateDto;
import se.david.api.core.order.dto.OrderDto;
import se.david.api.core.order.dto.OrderItemCreateDto;

import java.util.List;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestChannelBinderConfiguration.class)
@Testcontainers
class OrderServiceApplicationTests {

  @Container
  @SuppressWarnings("resource")
  static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
    .withDatabaseName("order_db");

  @DynamicPropertySource
  static void mysqlProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
    registry.add("eureka.client.enabled", () -> "false");
    registry.add("spring.cloud.discovery.enabled", () -> "false");
  }

  @Autowired
  private WebTestClient client;

  @Test
  @DisplayName("POST /orders persists a totalPrice computed from the order items")
  void createOrderPersistsTotalPriceComputedFromItems() {
    // Phase 0 regression: totalPrice must be computed (price * quantity summed), not left as 0.
    OrderCreateDto createDto = new OrderCreateDto(1,
      List.of(
        new OrderItemCreateDto(101, 2, 500),
        new OrderItemCreateDto(102, 3, 300)));

    OrderDto created = client.post().uri("/orders")
      .bodyValue(createDto)
      .exchange()
      .expectStatus().isOk()
      .expectBody(OrderDto.class)
      .returnResult()
      .getResponseBody();

    assert created != null;
    org.junit.jupiter.api.Assertions.assertEquals(1900, created.totalPrice());
    org.junit.jupiter.api.Assertions.assertEquals(2, created.orderItems().size());

    client.get().uri("/orders/{id}", created.id())
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.totalPrice").isEqualTo(1900)
      .jsonPath("$.orderItems.length()").isEqualTo(2)
      .jsonPath("$.orderItems[0].price").isEqualTo(500)
      .jsonPath("$.orderItems[1].price").isEqualTo(300);
  }

  @Test
  @DisplayName("GET /orders/{id} returns 404 for a non-existent order")
  void getOrderNonExistentReturnsNotFound() {
    client.get().uri("/orders/{id}", 999_999)
      .exchange()
      .expectStatus().isNotFound();
  }

  @Test
  @DisplayName("POST /orders returns 422 when orderItems is empty")
  void createOrderEmptyOrderItemsReturnsUnprocessableEntity() {
    OrderCreateDto invalid = new OrderCreateDto(1, List.of());

    client.post().uri("/orders")
      .bodyValue(invalid)
      .exchange()
      .expectStatus().isEqualTo(422);
  }

  @Test
  @DisplayName("DELETE /orders/{id} removes the order and its items")
  void deleteOrderRemovesOrderAndItsItems() {
    OrderCreateDto createDto = new OrderCreateDto(2, List.of(new OrderItemCreateDto(201, 1, 250)));

    OrderDto created = client.post().uri("/orders")
      .bodyValue(createDto)
      .exchange()
      .expectStatus().isOk()
      .expectBody(OrderDto.class)
      .returnResult()
      .getResponseBody();

    assert created != null;

    client.delete().uri("/orders/{id}", created.id())
      .exchange()
      .expectStatus().isOk();

    client.get().uri("/orders/{id}", created.id())
      .exchange()
      .expectStatus().isNotFound();
  }
}
