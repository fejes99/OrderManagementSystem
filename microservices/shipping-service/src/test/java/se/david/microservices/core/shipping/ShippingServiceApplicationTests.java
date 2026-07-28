package se.david.microservices.core.shipping;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.david.api.core.shipping.dto.ShippingCreateDto;
import se.david.api.event.Event;
import se.david.microservices.core.shipping.domain.entity.Shipping;

import static org.awaitility.Awaitility.await;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestChannelBinderConfiguration.class)
@Testcontainers
class ShippingServiceApplicationTests {

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
  @DisplayName("create and get a shipment end to end against real MongoDB")
  void createGetShipmentHappyPath() {
    ShippingCreateDto createDto = new ShippingCreateDto(1, "123 Main St");

    client.post().uri("/shipments")
      .bodyValue(createDto)
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.orderId").isEqualTo(1)
      .jsonPath("$.shippingAddress").isEqualTo("123 Main St")
      .jsonPath("$.status").isEqualTo("Dispatched");

    client.get().uri("/shipments/order/{orderId}", 1)
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.orderId").isEqualTo(1);
  }

  @Test
  @DisplayName("GET /shipments/order/{orderId} returns 404 for a non-existent shipment")
  void getShippingByOrderIdNonExistentReturnsNotFound() {
    client.get().uri("/shipments/order/{orderId}", 999_999)
      .exchange()
      .expectStatus().isNotFound();
  }

  @Test
  @DisplayName("PUT /shipments/order/{orderId} updates the shipment status")
  void updateShippingStatusViaRestEndpoint() {
    client.post().uri("/shipments")
      .bodyValue(new ShippingCreateDto(2, "456 Oak Ave"))
      .exchange()
      .expectStatus().isOk();

    client.put().uri("/shipments/order/{orderId}", 2)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue("DELIVERED")
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.status").isEqualTo("DELIVERED");
  }

  @Test
  @DisplayName("messageProcessor consumes a CREATE event and creates the shipment")
  void messageConsumerCreateEventCreatesShipment() {
    Shipping shipping = new Shipping(3001, "789 Pine Rd", "Dispatched");
    Event<Integer, Shipping> event = new Event<>(Event.Type.CREATE, 3001, shipping);
    inputDestination.send(MessageBuilder.withPayload(event).build(), "shipments");

    await().untilAsserted(() ->
      client.get().uri("/shipments/order/{orderId}", 3001)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.shippingAddress").isEqualTo("789 Pine Rd"));
  }

  @Test
  @DisplayName("messageProcessor consumes an UPDATE event and updates the shipment status")
  void messageConsumerUpdateEventUpdatesStatus() {
    client.post().uri("/shipments")
      .bodyValue(new ShippingCreateDto(4001, "1 Test Way"))
      .exchange()
      .expectStatus().isOk();

    Shipping update = new Shipping(4001, "1 Test Way", "DELIVERED");
    Event<Integer, Shipping> event = new Event<>(Event.Type.UPDATE, 4001, update);
    inputDestination.send(MessageBuilder.withPayload(event).build(), "shipments");

    await().untilAsserted(() ->
      client.get().uri("/shipments/order/{orderId}", 4001)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.status").isEqualTo("DELIVERED"));
  }
}
