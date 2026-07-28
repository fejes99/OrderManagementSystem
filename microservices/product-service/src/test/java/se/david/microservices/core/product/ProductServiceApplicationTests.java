package se.david.microservices.core.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.david.api.core.product.dto.ProductCreateDto;
import se.david.api.core.product.dto.ProductDto;
import se.david.api.core.product.dto.ProductUpdateDto;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class ProductServiceApplicationTests {

  @Container
  @SuppressWarnings("resource")
  static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
    .withDatabaseName("product_db");

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
  @DisplayName("create, get, update and delete a product end to end against real MySQL")
  void createGetUpdateDeleteProductHappyPath() {
    ProductCreateDto createDto = new ProductCreateDto("Test Product", "a description", 150);

    ProductDto created = client.post().uri("/products")
      .bodyValue(createDto)
      .exchange()
      .expectStatus().isOk()
      .expectBody(ProductDto.class)
      .returnResult()
      .getResponseBody();

    assert created != null;
    int productId = created.id();

    client.get().uri("/products/{id}", productId)
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.id").isEqualTo(productId)
      .jsonPath("$.name").isEqualTo("Test Product")
      .jsonPath("$.price").isEqualTo(150);

    ProductUpdateDto updateDto = new ProductUpdateDto("updated description", 200);
    client.put().uri("/products/{id}", productId)
      .bodyValue(updateDto)
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.description").isEqualTo("updated description")
      .jsonPath("$.price").isEqualTo(200);

    client.delete().uri("/products/{id}", productId)
      .exchange()
      .expectStatus().isOk();

    client.get().uri("/products/{id}", productId)
      .exchange()
      .expectStatus().isNotFound();
  }

  @Test
  @DisplayName("GET /products/{id} returns 404 for a non-existent product")
  void getProductNonExistentReturnsNotFound() {
    client.get().uri("/products/{id}", 999_999)
      .exchange()
      .expectStatus().isNotFound()
      .expectBody()
      .jsonPath("$.message").isEqualTo("Product with id 999999 not found");
  }

  @Test
  @DisplayName("GET /products/{id} returns 422 for a non-positive id")
  void getProductInvalidIdReturnsUnprocessableEntity() {
    client.get().uri("/products/{id}", -1)
      .exchange()
      .expectStatus().isEqualTo(422);
  }

  @Test
  @DisplayName("POST /products returns 422 for a blank name and non-positive price")
  void createProductBlankNameAndNonPositivePriceReturnsUnprocessableEntity() {
    ProductCreateDto invalid = new ProductCreateDto("", "desc", -5);

    client.post().uri("/products")
      .bodyValue(invalid)
      .exchange()
      .expectStatus().isEqualTo(422);
  }
}
