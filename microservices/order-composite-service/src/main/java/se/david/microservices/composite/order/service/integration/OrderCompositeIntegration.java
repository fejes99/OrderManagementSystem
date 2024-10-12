package se.david.microservices.composite.order.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
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
import se.david.api.event.Event;
import se.david.api.exceptions.InvalidInputException;
import se.david.api.exceptions.NotFoundException;
import se.david.util.http.HttpErrorInfo;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static java.util.logging.Level.FINE;

@Component
public class OrderCompositeIntegration implements ProductService, InventoryService, OrderService, ShippingService {
  private static final Logger LOG = LoggerFactory.getLogger(OrderCompositeIntegration.class);

  private final WebClient webClient;
  private final ObjectMapper mapper;

  private final String PRODUCT_SERVICE_URL;
  private final String INVENTORY_SERVICE_URL;
  private final String ORDER_SERVICE_URL;
  private final String SHIPPING_SERVICE_URL;

  private final StreamBridge streamBridge;
  private final Scheduler publishEventScheduler;

  @Autowired
  public OrderCompositeIntegration(
    @Qualifier("publishEventScheduler") Scheduler publishEventScheduler,
    StreamBridge streamBridge,
    WebClient.Builder webClientBuilder,
    ObjectMapper mapper,

    @Value("${app.product-service.host}") String productServiceHost,
    @Value("${app.product-service.port}") int  productServicePort,

    @Value("${app.inventory-service.host}") String inventoryServiceHost,
    @Value("${app.inventory-service.port}") int  inventoryServicePort,

    @Value("${app.order-service.host}") String orderServiceHost,
    @Value("${app.order-service.port}") int  orderServicePort,

    @Value("${app.shipping-service.host}") String shippingServiceHost,
    @Value("${app.shipping-service.port}") int  shippingServicePort) {
    this.publishEventScheduler = publishEventScheduler;
    this.streamBridge = streamBridge;
    this.webClient = webClientBuilder.build();
    this.mapper = mapper;
    PRODUCT_SERVICE_URL = "http://" + productServiceHost + ":" + productServicePort;
    INVENTORY_SERVICE_URL = "http://" + inventoryServiceHost + ":" + inventoryServicePort;
    ORDER_SERVICE_URL = "http://" + orderServiceHost + ":" + orderServicePort;
    SHIPPING_SERVICE_URL = "http://" + shippingServiceHost + ":" + shippingServicePort;
  }

  private <T> Flux<T> getFlux(String url, Class<T> responseType) {
    return webClient.get()
      .uri(url)
      .retrieve()
      .bodyToFlux(responseType)
      .doOnError(ex -> LOG.error("Error fetching from URL: {}", url, ex))
      .log(LOG.getName(), Level.FINE)
      .onErrorMap(WebClientResponseException.class, this::handleException);
  }

  private <T> Mono<T> getMono(String url, Class<T> responseType) {
    return webClient.get()
      .uri(url)
      .retrieve()
      .bodyToMono(responseType)
      .doOnError(ex -> LOG.error("Error fetching from URL: {}", url, ex))
      .log(LOG.getName(), Level.FINE)
      .onErrorMap(WebClientResponseException.class, this::handleException);
  }

  private <K, V, T> Mono<T> sendEventAndFetch(String bindingName, Event.Type eventType, K key, V payload, String fetchUrl, Class<T> responseType) {
    return sendEvent(bindingName, eventType, key, payload)
      .then(getMono(fetchUrl, responseType))
      .subscribeOn(publishEventScheduler);
  }

  private <K, V> Mono<Void> sendEvent(String bindingName, Event.Type eventType, K key, V payload) {
    Event<K, V> event = new Event<>(eventType, key, payload);
    return Mono.fromRunnable(() -> sendMessage(bindingName, event))
      .doOnError(ex -> LOG.error("Failed to send {} event for key: {}", eventType, key, ex)).then();
  }

  @Override
  public Flux<InventoryDto> getInventoryStocks() {
    return getFlux(INVENTORY_SERVICE_URL + "/inventories", InventoryDto.class);
  }

  @Override
  public Flux<OrderDto> getOrders() {
    return getFlux(ORDER_SERVICE_URL + "/orders", OrderDto.class);
  }

  @Override
  public Flux<ProductDto> getProducts() {
    return getFlux(PRODUCT_SERVICE_URL + "/products", ProductDto.class);
  }

  @Override
  public Flux<ProductDto> getProductsByIds(List<Integer> ids) {
    String idsParam = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    String url = PRODUCT_SERVICE_URL + "/products/byIds?ids=" + idsParam;

    return getFlux(url, ProductDto.class);
  }

  @Override
  public Flux<ShippingDto> getShipments() {
    return getFlux(SHIPPING_SERVICE_URL + "/shipments", ShippingDto.class);
  }

  @Override
  public Flux<ShippingDto> getShipmentsByOrderIds(List<Integer> orderIds) {
    String url = SHIPPING_SERVICE_URL + "/shipments";
    StringBuilder urlBuilder = new StringBuilder(url + "/byOrdersIds?");

    for (Integer orderId : orderIds) {
      if (urlBuilder.length() > url.length() + "/byOrdersIds?".length()) {
        urlBuilder.append("&");
      }
      urlBuilder.append("orderIds=").append(orderId);
    }

    url = urlBuilder.toString();

    LOG.debug("getShipmentsByOrderIds: Fetching shipments by order IDs: {} from URL: {}", orderIds, url);

    return getFlux(url, ShippingDto.class);
  }

  @Override
  public Mono<ShippingDto> getShippingByOrderId(int orderId) {
    return getMono(SHIPPING_SERVICE_URL + "/shipments/order/" + orderId, ShippingDto.class);
  }

  @Override
  public Mono<ShippingDto> createShippingOrder(ShippingCreateDto shippingCreateDto) {
    String url = PRODUCT_SERVICE_URL + "/orders/" + shippingCreateDto.orderId();
    return sendEventAndFetch("shipments-out-0", Event.Type.CREATE, shippingCreateDto.orderId(), shippingCreateDto, url, ShippingDto.class);
  }

  @Override
  public Mono<ShippingDto> updateShippingStatusByOrderId(int orderId, String status) {
    String url = SHIPPING_SERVICE_URL + "/shipments/order/" + orderId;
    return sendEventAndFetch("shipments-out-0", Event.Type.UPDATE, orderId, status, url, ShippingDto.class);
  }

  @Override
  public Mono<ProductDto> getProduct(int productId) {
    return getMono(PRODUCT_SERVICE_URL + "/products/" + productId, ProductDto.class);
  }

  @Override
  public Mono<ProductDto> createProduct(ProductCreateDto productCreateDto) {
    return webClient.post()
      .uri(PRODUCT_SERVICE_URL + "/products")
      .bodyValue(productCreateDto)
      .retrieve()
      .bodyToMono(ProductDto.class)
      .doOnSuccess(productDto -> LOG.debug("createProduct: Created a product with id: {}", productDto.id()))
      .doOnError(ex -> LOG.error("Error creating product", ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Mono<ProductDto> updateProduct(int productId, ProductUpdateDto productUpdateDto) {
    return webClient.put()
      .uri(PRODUCT_SERVICE_URL + "/products/" + productId)
      .bodyValue(productUpdateDto)
      .retrieve()
      .bodyToMono(Void.class)
      .then(getProduct(productId))
      .doOnSuccess(updatedProduct -> LOG.debug("updateProduct: Updated product with ID: {}", updatedProduct.id()))
      .doOnError(ex -> LOG.error("Error updating product with ID: {}", productId, ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Mono<Void> deleteProduct(int productId) {
    return webClient.delete()
      .uri(PRODUCT_SERVICE_URL + "/products/" + productId)
      .retrieve()
      .bodyToMono(Void.class)
      .doOnSuccess(unused -> LOG.debug("deleteProduct: Successfully deleted product with ID: {}", productId))
      .doOnError(ex -> LOG.error("Error deleting product with ID: {}", productId, ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Flux<OrderDto> getOrdersByUser(int userId) {
    return getFlux(ORDER_SERVICE_URL + "/orders/user/" + userId, OrderDto.class);
  }

  @Override
  public Mono<OrderDto> getOrder(int orderId) {
    return getMono(PRODUCT_SERVICE_URL + "/orders/" + orderId, OrderDto.class);
  }

  @Override
  public Mono<OrderDto> createOrder(OrderCreateDto orderCreateDto) {
    return webClient.post()
      .uri(ORDER_SERVICE_URL + "/orders")
      .bodyValue(orderCreateDto)
      .retrieve()
      .bodyToMono(OrderDto.class)
      .doOnSuccess(orderDto -> LOG.debug("createOrder: Created an order with id: {}", orderDto.id()))
      .doOnError(ex -> LOG.error("Error creating order", ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Mono<OrderDto> updateOrder(int orderId, OrderUpdateDto orderUpdateDto) {
    return sendEventAndFetch("orders-out-0", Event.Type.UPDATE, orderId, orderUpdateDto, PRODUCT_SERVICE_URL + "/orders/" + orderId, OrderDto.class);
  }

  @Override
  public Mono<Void> deleteOrder(int orderId) {
    return webClient.delete()
      .uri(PRODUCT_SERVICE_URL + "/orders/" + orderId)
      .retrieve()
      .bodyToMono(Void.class)
      .doOnSuccess(unused -> LOG.debug("deleteOrder: Successfully deleted order with ID: {}", orderId))
      .doOnError(ex -> LOG.error("Error deleting order with ID: {}", orderId, ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Mono<InventoryDto> getInventoryStock(int productId) {
    return getMono(INVENTORY_SERVICE_URL + "/inventories/" + productId, InventoryDto.class);
  }

  @Override
  public Mono<InventoryDto> createInventoryStock(InventoryCreateDto inventoryCreateDto) {
    return webClient.post()
      .uri(INVENTORY_SERVICE_URL + "/inventories")
      .bodyValue(inventoryCreateDto)
      .retrieve()
      .bodyToMono(InventoryDto.class)
      .doOnSuccess(createdInventoryDto -> LOG.debug("createInventoryStock: Created inventory stock with productId: {}", createdInventoryDto.productId()))
      .doOnError(ex -> LOG.error("Error creating inventory stock", ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Mono<Void> deleteInventoryStock(int productId) {
    return webClient.delete()
      .uri(INVENTORY_SERVICE_URL + "/inventories/" + productId)
      .retrieve()
      .bodyToMono(Void.class)
      .doOnError(ex -> LOG.error("Error deleting inventory stock for productId: {}", productId, ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Mono<InventoryDto> increaseStock(InventoryStockAdjustmentRequestDto inventoryIncreaseDto) {
    return sendEventAndFetch("inventories-out-0", Event.Type.INCREASE_STOCK, inventoryIncreaseDto.productId(), inventoryIncreaseDto, INVENTORY_SERVICE_URL + "/inventories/" + inventoryIncreaseDto.productId(), InventoryDto.class);
  }

  @Override
  public Mono<Void> reduceStocks(List<InventoryStockAdjustmentRequestDto> inventoryReduceDtos) {
    return sendEvent("inventories-out-0", Event.Type.REDUCE_STOCKS, null, inventoryReduceDtos);
  }

  public Mono<Health> getInventoryHealth() { return getHealth(INVENTORY_SERVICE_URL); }
  public Mono<Health> getOrderHealth() {
    return getHealth(ORDER_SERVICE_URL);
  }
  public Mono<Health> getShippingHealth() {
    return getHealth(SHIPPING_SERVICE_URL);
  }

  private Mono<Health> getHealth(String url) {
    url += "/actuator/health";
    LOG.debug("Will call the Health API on URL: {}", url);
    return webClient.get()
      .uri(url)
      .retrieve()
      .bodyToMono(String.class)
      .map(s -> new Health.Builder().up().build())
      .onErrorResume(ex -> Mono.just(new Health.Builder().down(ex).build()))
      .log(LOG.getName(), FINE);
  }

  private void sendMessage(String bindingName, Event event) {
    LOG.debug("Sending a {} message to {}", event.getEventType(), bindingName);
    Message message = MessageBuilder.withPayload(event)
      .setHeader("partitionKey", event.getKey())
      .build();
    streamBridge.send(bindingName, message);
  }

  private Throwable handleException(Throwable ex) {
    if(!(ex instanceof WebClientResponseException wcre)) {
      LOG.warn("Got a unexpected error: {}, will rethrow it", ex.toString());
      return ex;
    }

    switch (HttpStatus.resolve(wcre.getStatusCode().value())) {

      case NOT_FOUND:
        return new NotFoundException(getErrorMessage(wcre));

      case UNPROCESSABLE_ENTITY:
        return new InvalidInputException(getErrorMessage(wcre));

      default:
        LOG.warn("Got an unexpected HTTP error: {}, will rethrow it", wcre.getStatusCode());
        LOG.warn("Error body: {}", wcre.getResponseBodyAsString());
        return ex;
    }
  }

  private String getErrorMessage(WebClientResponseException ex) {
    try {
      return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
    } catch (IOException ioex) {
      return ex.getMessage();
    }
  }
}
