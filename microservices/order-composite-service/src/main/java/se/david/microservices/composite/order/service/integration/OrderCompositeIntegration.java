package se.david.microservices.composite.order.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.david.api.core.inventory.dto.InventoryCreateDto;
import se.david.api.core.inventory.dto.InventoryDto;
import se.david.api.core.inventory.dto.InventoryStockAdjustmentRequestDto;
import se.david.api.core.inventory.service.InventoryService;
import se.david.api.core.order.dto.OrderCreateDto;
import se.david.api.core.order.dto.OrderDto;
import se.david.api.core.order.dto.OrderUpdateDto;
import se.david.api.core.order.service.OrderService;
import se.david.api.core.product.dto.ProductCreateDto;
import se.david.api.core.product.dto.ProductDto;
import se.david.api.core.product.dto.ProductUpdateDto;
import se.david.api.core.product.service.ProductService;
import se.david.api.core.shipping.dto.ShippingCreateDto;
import se.david.api.core.shipping.dto.ShippingDto;
import se.david.api.core.shipping.service.ShippingService;

import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Component
public class OrderCompositeIntegration implements ProductService, InventoryService, OrderService, ShippingService {
  private static final Logger LOG = LoggerFactory.getLogger(OrderCompositeIntegration.class);

  private final WebClient webClient;
  private final RestTemplate restTemplate;
  private final ObjectMapper mapper;

  private final String productServiceUrl;
  private final String inventoryServiceUrl;
  private final String orderServiceUrl;
  private final String shippingServiceUrl;

  @Autowired
  public OrderCompositeIntegration(
    WebClient.Builder webClientBuilder,
    RestTemplate restTemplate,
    ObjectMapper mapper,
    @Value("${app.product-service.host}") String productServiceHost,
    @Value("${app.product-service.port}") String productServicePort,
    @Value("${app.inventory-service.host}") String inventoryServiceHost,
    @Value("${app.inventory-service.port}") String inventoryServicePort,
    @Value("${app.order-service.host}") String orderServiceHost,
    @Value("${app.order-service.port}") String orderServicePort,
    @Value("${app.shipping-service.host}") String shippingServiceHost,
    @Value("${app.shipping-service.port}") String shippingServicePort) {
    this.webClient = webClientBuilder.build();

    this.restTemplate = restTemplate;
    this.mapper = mapper;

    productServiceUrl = "http://" + productServiceHost + ":" + productServicePort + "/products";
    inventoryServiceUrl = "http://" + inventoryServiceHost + ":" + inventoryServicePort + "/inventories";
    orderServiceUrl = "http://" + orderServiceHost + ":" + orderServicePort + "/orders";
    shippingServiceUrl = "http://" + shippingServiceHost + ":" + shippingServicePort + "/shipments";
  }

  @Override
  public Flux<InventoryDto> getInventoryStocks() {
    LOG.debug("getInventoryStocks: Fetching inventory stocks from URL: {}", inventoryServiceUrl);

    return webClient.get()
      .uri(inventoryServiceUrl)
      .retrieve()
      .bodyToFlux(InventoryDto.class)
      .doOnError(ex -> LOG.error("Error fetching inventory stocks from URL: {}", inventoryServiceUrl, ex))
      .log(LOG.getName(), Level.FINE);
  }


  @Override
  public Flux<OrderDto> getOrders() {
    LOG.debug("getOrders: Fetching orders from URL: {}", orderServiceUrl);

    return webClient.get()
      .uri(orderServiceUrl)
      .retrieve()
      .bodyToFlux(OrderDto.class)
      .doOnError(ex -> LOG.error("Error fetching orders from URL: {}", orderServiceUrl, ex))
      .log(LOG.getName(), Level.FINE);
  }


  @Override
  public Flux<ProductDto> getProducts() {
    LOG.debug("getProducts: Fetching products from URL: {}", productServiceUrl);

    return webClient.get()
      .uri(productServiceUrl)
      .retrieve()
      .bodyToFlux(ProductDto.class)
      .doOnError(ex -> LOG.error("Error fetching products", ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Flux<ProductDto> getProductsByIds(List<Integer> ids) {
    String idsParam = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    String url = productServiceUrl + "/byIds?ids=" + idsParam;

    LOG.debug("Fetching products by IDs: {} from URL: {}", idsParam, url);

    return webClient.get()
      .uri(url)
      .retrieve()
      .bodyToFlux(ProductDto.class)
      .doOnError(ex -> LOG.error("Error fetching products by IDs: {}", idsParam, ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Flux<ShippingDto> getShipments() {
    LOG.debug("getShipments: Fetching shipments from URL: {}", shippingServiceUrl);

    return webClient.get()
      .uri(shippingServiceUrl)
      .retrieve()
      .bodyToFlux(ShippingDto.class)
      .doOnError(ex -> LOG.error("Error fetching shipments", ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Flux<ShippingDto> getShipmentsByOrderIds(List<Integer> orderIds) {
    StringBuilder urlBuilder = new StringBuilder(shippingServiceUrl + "/byOrdersIds?");

    for (Integer orderId : orderIds) {
      if (urlBuilder.length() > shippingServiceUrl.length() + "/byOrdersIds?".length()) {
        urlBuilder.append("&");
      }
      urlBuilder.append("orderIds=").append(orderId);
    }

    String url = urlBuilder.toString();

    LOG.debug("getShipmentsByOrderIds: Fetching shipments by order IDs: {} from URL: {}", orderIds, url);

    return webClient.get()
      .uri(url)
      .retrieve()
      .bodyToFlux(ShippingDto.class)
      .doOnError(ex -> LOG.error("Error fetching shipments by orderIds", ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Mono<ShippingDto> getShippingByOrderId(int orderId) {
    String url = shippingServiceUrl + "/order/" + orderId;
    LOG.debug("getShippingByOrderId: Fetching shipping details for order ID: {}", orderId);

    return webClient.get()
      .uri(url)
      .retrieve()
      .bodyToMono(ShippingDto.class)
      .doOnError(ex -> LOG.error("Error fetching shipping details for orderId: {}", orderId, ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Mono<ShippingDto> createShippingOrder(ShippingCreateDto shippingCreateDto) {
    LOG.debug("createShippingOrder: Creating new shipping order at URL: {}", shippingServiceUrl);

    return webClient.post()
      .uri(shippingServiceUrl)
      .bodyValue(shippingCreateDto)
      .retrieve()
      .bodyToMono(ShippingDto.class)
      .doOnSuccess(shippingDto -> LOG.debug("createShippingOrder: Created a shipping with orderId: {}", shippingDto.orderId()))
      .doOnError(ex -> LOG.error("Error creating shipping order", ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Mono<ShippingDto> updateShippingStatusByOrderId(int orderId, String status) {
    String url = shippingServiceUrl + "/order/" + orderId;
    LOG.debug("updateShippingStatusByOrderId: Updating shipping status for order ID: {} to status: {}", orderId, status);

    return webClient.put()
      .uri(url)
      .bodyValue(status)
      .retrieve()
      .bodyToMono(ShippingDto.class)
      .doOnSuccess(shippingDto -> LOG.debug("updateShippingStatusByOrderId: Updated shipping status for orderId: {}", orderId))
      .doOnError(ex -> LOG.error("Error updating shipping status for orderId: {}", orderId, ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Mono<ProductDto> getProduct(int productId) {
    String url = productServiceUrl + "/" + productId;
    LOG.debug("getProduct: Fetching product details for product ID: {}", productId);

    return webClient.get()
      .uri(url)
      .retrieve()
      .bodyToMono(ProductDto.class)
      .doOnError(ex -> LOG.error("Error fetching product details for productId: {}", productId, ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Mono<ProductDto> createProduct(ProductCreateDto productCreateDto) {
    LOG.debug("createProduct: Creating new product at URL: {}", productServiceUrl);

    return webClient.post()
      .uri(productServiceUrl)
      .bodyValue(productCreateDto)
      .retrieve()
      .bodyToMono(ProductDto.class)
      .doOnSuccess(productDto -> LOG.debug("createProduct: Created a product with id: {}", productDto.id()))
      .doOnError(ex -> LOG.error("Error creating product", ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Mono<ProductDto> updateProduct(int productId, ProductUpdateDto productUpdateDto) {
    String url = productServiceUrl + "/" + productId;
    LOG.debug("updateProduct: Updating product with ID: {}", productId);

    return webClient.put()
      .uri(url)
      .bodyValue(productUpdateDto)
      .retrieve()
      .bodyToMono(Void.class)  // WebClient PUT doesn't expect a body to be returned
      .then(getProduct(productId))  // After updating, we return the updated product
      .doOnSuccess(updatedProduct -> LOG.debug("updateProduct: Updated product with ID: {}", updatedProduct.id()))
      .doOnError(ex -> LOG.error("Error updating product with ID: {}", productId, ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Mono<Void> deleteProduct(int productId) {
    String url = productServiceUrl + "/" + productId;
    LOG.debug("deleteProduct: Deleting product with ID: {} from URL: {}", productId, url);

    return webClient.delete()
      .uri(url)
      .retrieve()
      .bodyToMono(Void.class)  // DELETE usually returns no body, so we expect Mono<Void>
      .doOnSuccess(unused -> LOG.debug("deleteProduct: Successfully deleted product with ID: {}", productId))
      .doOnError(ex -> LOG.error("Error deleting product with ID: {}", productId, ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Flux<OrderDto> getOrdersByUser(int userId) {
    String url = orderServiceUrl + "/user/" + userId;
    LOG.debug("getOrdersByUser: Fetching orders for user ID: {}", userId);

    return webClient.get()
      .uri(url)
      .retrieve()
      .bodyToFlux(OrderDto.class)
      .doOnError(ex -> LOG.error("Error fetching orders for user ID: {}", userId, ex))
      .log(LOG.getName(), Level.FINE);
  }


  @Override
  public Mono<OrderDto> getOrder(int orderId) {
    String url = orderServiceUrl + "/" + orderId;
    LOG.debug("getOrder: Fetching order details for order ID: {}", orderId);

    return webClient.get()
      .uri(url)
      .retrieve()
      .bodyToMono(OrderDto.class)
      .doOnError(ex -> LOG.error("Error fetching order details for order ID: {}", orderId, ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Mono<OrderDto> createOrder(OrderCreateDto orderCreateDto) {
    LOG.debug("createOrder: Creating new order at URL: {}", orderServiceUrl);

    return webClient.post()
      .uri(orderServiceUrl)
      .bodyValue(orderCreateDto)
      .retrieve()
      .bodyToMono(OrderDto.class)
      .doOnSuccess(orderDto -> LOG.debug("createOrder: Created an order with id: {}", orderDto.id()))
      .doOnError(ex -> LOG.error("Error creating order", ex))
      .log(LOG.getName(), Level.FINE);
  }


  @Override
  public Mono<OrderDto> updateOrder(int orderId, OrderUpdateDto orderUpdateDto) {
    String url = orderServiceUrl + "/" + orderId;
    LOG.debug("updateOrder: Updating order with ID: {}", orderId);

    return webClient.put()
      .uri(url)
      .bodyValue(orderUpdateDto)
      .retrieve()
      .bodyToMono(Void.class)  // WebClient PUT typically returns no body
      .then(getOrder(orderId))  // Retrieve the updated order after the PUT operation
      .doOnSuccess(updatedOrder -> LOG.debug("updateOrder: Updated order with ID: {}", updatedOrder.id()))
      .doOnError(ex -> LOG.error("Error updating order with ID: {}", orderId, ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Mono<Void> deleteOrder(int orderId) {
    String url = orderServiceUrl + "/" + orderId;
    LOG.debug("deleteOrder: Deleting order with ID: {} from URL: {}", orderId, url);

    return webClient.delete()
      .uri(url)
      .retrieve()
      .bodyToMono(Void.class)
      .doOnSuccess(unused -> LOG.debug("deleteOrder: Successfully deleted order with ID: {}", orderId))
      .doOnError(ex -> LOG.error("Error deleting order with ID: {}", orderId, ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Mono<InventoryDto> getInventoryStock(int productId) {
    String url = inventoryServiceUrl + "/" + productId;
    LOG.debug("getInventoryStock: Fetching inventory stock for product ID: {}", productId);

    return webClient.get()
      .uri(url)
      .retrieve()
      .bodyToMono(InventoryDto.class)
      .doOnError(ex -> LOG.error("Error fetching inventory stock for productId: {}", productId, ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Mono<InventoryDto> createInventoryStock(InventoryCreateDto inventoryCreateDto) {
    LOG.debug("createInventoryStock: Creating inventory stock at URL: {}", inventoryServiceUrl);

    return webClient.post()
      .uri(inventoryServiceUrl)
      .bodyValue(inventoryCreateDto)
      .retrieve()
      .bodyToMono(InventoryDto.class)
      .doOnSuccess(createdInventoryDto -> LOG.debug("createInventoryStock: Created inventory stock with productId: {}", createdInventoryDto.productId()))
      .doOnError(ex -> LOG.error("Error creating inventory stock", ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Mono<Void> deleteInventoryStock(int productId) {
    String url = inventoryServiceUrl + "/" + productId;
    LOG.debug("deleteInventoryStock: Deleting inventory stock with product ID: {}", productId);

    return webClient.delete()
      .uri(url)
      .retrieve()
      .bodyToMono(Void.class)
      .doOnError(ex -> LOG.error("Error deleting inventory stock for productId: {}", productId, ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Mono<InventoryDto> increaseStock(InventoryStockAdjustmentRequestDto inventoryIncreaseDto) {
    String url = inventoryServiceUrl + "/increaseStock";
    LOG.debug("increaseStock: Increasing stock for the requested product");

    return webClient.put()
      .uri(url)
      .bodyValue(inventoryIncreaseDto)
      .retrieve()
      .bodyToMono(InventoryDto.class)
      .doOnError(ex -> LOG.error("Error increasing stock", ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Mono<Void> reduceStocks(List<InventoryStockAdjustmentRequestDto> inventoryReduceDtos) {
    String url = inventoryServiceUrl + "/reduceStock";
    LOG.debug("reduceStocks: Reducing stocks for the requested products");

    return webClient.put()
      .uri(url)
      .bodyValue(inventoryReduceDtos)
      .retrieve()
      .bodyToMono(Void.class)
      .doOnError(ex -> LOG.error("Error reducing stocks", ex))
      .log(LOG.getName(), Level.FINE);
  }
}
