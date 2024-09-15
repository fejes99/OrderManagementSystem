package se.david.microservices.composite.order.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import se.david.api.core.inventory.dto.InventoryDto;
import se.david.api.core.inventory.dto.InventoryStockAdjustmentRequestDto;
import se.david.api.core.inventory.service.InventoryService;
import se.david.api.core.order.dto.OrderCreateDto;
import se.david.api.core.order.dto.OrderDto;
import se.david.api.core.order.service.OrderService;
import se.david.api.core.product.dto.ProductDto;
import se.david.api.core.product.service.ProductService;
import se.david.api.core.shipping.dto.ShippingCreateDto;
import se.david.api.core.shipping.dto.ShippingDto;
import se.david.api.core.shipping.service.ShippingService;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderCompositeIntegration implements ProductService, InventoryService, OrderService, ShippingService {
  private static final Logger LOG = LoggerFactory.getLogger(OrderCompositeIntegration.class);

  private final RestTemplate restTemplate;
  private final ObjectMapper mapper;

  private final String productServiceUrl;
  private final String inventoryServiceUrl;
  private final String orderServiceUrl;
  private final String shippingServiceUrl;

  @Autowired
  public OrderCompositeIntegration(
    RestTemplate restTemplate,
    ObjectMapper mapper,
    @Value("${app.product-service.host}")
    String productServiceHost,
    @Value("${app.product-service.port}")
    String productServicePort,
    @Value("${app.inventory-service.host}")
    String inventoryServiceHost,
    @Value("${app.inventory-service.port}")
    String inventoryServicePort,
    @Value("${app.order-service.host}")
    String orderServiceHost,
    @Value("${app.order-service.port}")
    String orderServicePort,
    @Value("${app.shipping-service.host}")
    String shippingServiceHost,
    @Value("${app.shipping-service.port}")
    String shippingServicePort) {
    this.restTemplate = restTemplate;
    this.mapper = mapper;

    productServiceUrl = "http://" + productServiceHost + ":" + productServicePort + "/products/";
    inventoryServiceUrl = "http://" + inventoryServiceHost + ":" + inventoryServicePort + "/inventories/";
    orderServiceUrl = "http://" + orderServiceHost + ":" + orderServicePort + "/orders/";
    shippingServiceUrl = "http://" + shippingServiceHost + ":" + shippingServicePort + "/shipments/";
  }

  @Override
  public List<InventoryDto> getInventoryStocks() {
    return restTemplate.exchange(inventoryServiceUrl, HttpMethod.GET, null,
      new ParameterizedTypeReference<List<InventoryDto>>() {
      }).getBody();
  }

  @Override
  public List<OrderDto> getOrders() {
    return restTemplate.exchange(orderServiceUrl, HttpMethod.GET, null,
      new ParameterizedTypeReference<List<OrderDto>>() {
      }).getBody();
  }

  @Override
  public List<ProductDto> getProducts() {
    return restTemplate.exchange(productServiceUrl, HttpMethod.GET, null,
      new ParameterizedTypeReference<List<ProductDto>>() {
      }).getBody();
  }

  @Override
  public List<ProductDto> getProductsByIds(List<Integer> ids) {
    String idsParam = ids.stream()
      .map(String::valueOf)
      .collect(Collectors.joining(","));

    // Add the ids as a query parameter in the URL
    String url = productServiceUrl + "byIds?ids=" + idsParam;

    return restTemplate.exchange(url, HttpMethod.GET, null,
      new ParameterizedTypeReference<List<ProductDto>>() {
      }).getBody();
  }


  @Override
  public List<ShippingDto> getShipments() {
    return restTemplate.exchange(shippingServiceUrl, HttpMethod.GET, null,
      new ParameterizedTypeReference<List<ShippingDto>>() {
      }).getBody();
  }

  @Override
  public List<ShippingDto> getShipmentsByIds(List<Integer> ids) {
    String idsParam = ids.stream()
      .map(String::valueOf)
      .collect(Collectors.joining(","));

    // Add the ids as a query parameter in the URL
    String url = shippingServiceUrl + "byIds?ids=" + idsParam;

    return restTemplate.exchange(url, HttpMethod.GET, null,
      new ParameterizedTypeReference<List<ShippingDto>>() {
      }).getBody();
  }

  @Override
  public ShippingDto getShipping(int orderId) {
    String url = shippingServiceUrl + orderId;
    return restTemplate.getForObject(url, ShippingDto.class);
  }

  @Override
  public ShippingDto createShippingOrder(ShippingCreateDto shipping) {
    String url = shippingServiceUrl;
    LOG.debug("Will post a new shipping to URL: {}", url);

    ShippingDto shippingDto = restTemplate.postForObject(url, shipping, ShippingDto.class);
    assert shippingDto != null;
    LOG.debug("Created a shipping with orderId: {}", shippingDto.orderId());

    return shippingDto;
  }

  @Override
  public ShippingDto updateShippingStatus(int orderId, String status) {
    String url = shippingServiceUrl + "/" + orderId + "/status";
    LOG.debug("Will update the shipping status for orderId: {} to status: {}", orderId, status);

    return restTemplate.patchForObject(url, status, ShippingDto.class);
  }


  @Override
  public void deleteShipping(int shippingId) {
    String url = shippingServiceUrl + shippingId;
    LOG.debug("Will call the deleteShipping API on URL: {}", url);

    restTemplate.delete(url);
  }

  @Override
  public ProductDto getProduct(int productId) {
    String url = productServiceUrl + productId;
    return restTemplate.getForObject(url, ProductDto.class);
  }

  @Override
  public ProductDto createProduct(ProductDto product) {
    String url = productServiceUrl;
    LOG.debug("Will post a new product to URL: {}", url);

    ProductDto productDto = restTemplate.postForObject(url, product, ProductDto.class);
    assert productDto != null;
    LOG.debug("Created a product with id: {}", productDto.id());

    return productDto;
  }

  @Override
  public ProductDto updateProduct(int productId, ProductDto product) {
    String url = productServiceUrl + productId;
    LOG.debug("Will update the product with id: {}", productId);

    restTemplate.put(url, product);
    return getProduct(productId);
  }


  @Override
  public void deleteProduct(int productId) {
    String url = productServiceUrl + productId;
    LOG.debug("Will call the deleteProduct API on URL: {}", url);

    restTemplate.delete(url);
  }

  @Override
  public List<OrderDto> getOrdersByUser(int userId) {
    return List.of();
  }

  @Override
  public OrderDto getOrder(int orderId) {
    String url = orderServiceUrl + orderId;
    return restTemplate.getForObject(url, OrderDto.class);
  }

  @Override
  public OrderDto createOrder(OrderCreateDto order) {
    String url = orderServiceUrl;
    LOG.debug("Will post a new order to URL: {}", url);

    OrderDto orderDto = restTemplate.postForObject(url, order, OrderDto.class);
    assert orderDto != null;
    LOG.debug("Created a order with id: {}", orderDto.id());

    return orderDto;
  }

  @Override
  public OrderDto updateOrder(int orderId, OrderDto order) {
    String url = orderServiceUrl + orderId;
    LOG.debug("Will update the order with id: {}", orderId);

    restTemplate.put(url, order);
    return getOrder(orderId);
  }


  @Override
  public void deleteOrder(int orderId) {
    String url = orderServiceUrl + orderId;
    LOG.debug("Will call the deleteOrder API on URL: {}", url);

    restTemplate.delete(url);
  }

  @Override
  public InventoryDto getInventoryStock(int productId) {
    String url = inventoryServiceUrl + productId;
    return restTemplate.getForObject(url, InventoryDto.class);
  }

  @Override
  public InventoryDto createInventoryStock(InventoryDto inventoryCreateRequest) {
    String url = inventoryServiceUrl;
    LOG.debug("Will post a new inventory stock to URL: {}", url);

    InventoryDto inventoryDto = restTemplate.postForObject(url, inventoryCreateRequest, InventoryDto.class);
    assert inventoryDto != null;
    LOG.debug("Created a inventory with productOd: {}", inventoryDto.productId());

    return inventoryDto;
  }

  @Override
  public void deleteInventoryStock(int productId) {
    String url = inventoryServiceUrl + productId;
    LOG.debug("Will call the deleteInventoryStock API on URL: {}", url);

    restTemplate.delete(url);
  }

  @Override
  public InventoryDto increaseStock(InventoryStockAdjustmentRequestDto inventoryIncreaseRequest) {
    String url = inventoryServiceUrl + "increaseStock";
    LOG.debug("Will increase stock for the requested product");

    return restTemplate.patchForObject(url, inventoryIncreaseRequest, InventoryDto.class);
  }

  @Override
  public void reduceStock(List<InventoryStockAdjustmentRequestDto> inventoryReduceRequests) {
    String url = inventoryServiceUrl + "reduceStock";
    LOG.debug("Will reduce stock for the requested products");

    restTemplate.put(url, inventoryReduceRequests, Void.class);
  }
}
