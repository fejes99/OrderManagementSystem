package se.david.microservices.core.inventory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.david.api.core.inventory.dto.InventoryCreateDto;
import se.david.api.core.inventory.dto.InventoryDto;
import se.david.api.event.Event;
import se.david.microservices.core.inventory.domain.entity.Inventory;

import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestChannelBinderConfiguration.class)
@Testcontainers
class InventoryServiceApplicationTests {

  @Container
  @SuppressWarnings("resource")
  static final MongoDBContainer mongo = new MongoDBContainer("mongo:6.0");

  @DynamicPropertySource
  static void mongoProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    registry.add("eureka.client.enabled", () -> "false");
    registry.add("spring.cloud.discovery.enabled", () -> "false");
  }

  @Autowired
  private WebTestClient client;

  @Autowired
  private InputDestination inputDestination;

  @Test
  @DisplayName("create, get and delete an inventory stock record end to end against real MongoDB")
  void createGetDeleteInventoryStockHappyPath() {
    InventoryCreateDto createDto = new InventoryCreateDto(1001, 20);

    client.post().uri("/inventories")
      .bodyValue(createDto)
      .exchange()
      .expectStatus().isOk()
      .expectBody(InventoryDto.class);

    client.get().uri("/inventories/{productId}", 1001)
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.productId").isEqualTo(1001)
      .jsonPath("$.quantity").isEqualTo(20);

    client.delete().uri("/inventories/{productId}", 1001)
      .exchange()
      .expectStatus().isOk();

    client.get().uri("/inventories/{productId}", 1001)
      .exchange()
      .expectStatus().isNotFound();
  }

  @Test
  @DisplayName("GET /inventories/{productId} returns 404 for a non-existent record")
  void getInventoryStockNonExistentReturnsNotFound() {
    client.get().uri("/inventories/{productId}", 999_999)
      .exchange()
      .expectStatus().isNotFound();
  }

  @Test
  @DisplayName("PUT /inventories/increaseStock adds the requested quantity")
  void increaseStockViaRestEndpointAddsQuantity() {
    client.post().uri("/inventories")
      .bodyValue(new InventoryCreateDto(2001, 10))
      .exchange()
      .expectStatus().isOk();

    client.put().uri("/inventories/increaseStock")
      .bodyValue(new se.david.api.core.inventory.dto.InventoryStockAdjustmentRequestDto(2001, 5))
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.quantity").isEqualTo(15);
  }

  @Test
  @DisplayName("messageProcessor consumes an INCREASE_STOCK event and updates the quantity")
  void messageConsumerIncreaseStockEventUpdatesQuantity() {
    client.post().uri("/inventories")
      .bodyValue(new InventoryCreateDto(3001, 10))
      .exchange()
      .expectStatus().isOk();

    Event<Integer, Inventory> event = new Event<>(Event.Type.INCREASE_STOCK, 3001, new Inventory(3001, 7));
    inputDestination.send(MessageBuilder.withPayload(event).build(), "inventories");

    await().untilAsserted(() ->
      client.get().uri("/inventories/{productId}", 3001)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.quantity").isEqualTo(17));
  }

  @Test
  @DisplayName("messageProcessor consumes a REDUCE_STOCKS event and updates every item's quantity")
  void messageConsumerReduceStocksEventUpdatesEachQuantity() {
    client.post().uri("/inventories")
      .bodyValue(new InventoryCreateDto(4001, 10))
      .exchange()
      .expectStatus().isOk();
    client.post().uri("/inventories")
      .bodyValue(new InventoryCreateDto(4002, 5))
      .exchange()
      .expectStatus().isOk();

    List<Inventory> reductions = List.of(new Inventory(4001, 4), new Inventory(4002, 5));
    Event<Integer, Inventory> event = new Event<>(Event.Type.REDUCE_STOCKS, null, reductions);
    inputDestination.send(MessageBuilder.withPayload(event).build(), "inventories");

    await().untilAsserted(() ->
      client.get().uri("/inventories/{productId}", 4001)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.quantity").isEqualTo(6));

    await().untilAsserted(() ->
      client.get().uri("/inventories/{productId}", 4002)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.quantity").isEqualTo(0));
  }
}
