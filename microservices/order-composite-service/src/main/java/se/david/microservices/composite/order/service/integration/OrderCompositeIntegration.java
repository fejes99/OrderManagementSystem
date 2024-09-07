package se.david.microservices.composite.order.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import se.david.api.core.inventory.dto.InventoryDto;
import se.david.api.core.inventory.service.InventoryService;
import se.david.api.core.order.dto.OrderDto;
import se.david.api.core.order.service.OrderService;
import se.david.api.core.product.dto.ProductDto;
import se.david.api.core.product.service.ProductService;
import se.david.api.core.shipping.dto.ShippingDto;
import se.david.api.core.shipping.service.ShippingService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderCompositeIntegration implements ProductService, InventoryService, OrderService, ShippingService {
  private final RestTemplate restTemplate;
  private final ObjectMapper mapper;

  private final String productServiceUrl;
  private final String inventoryServiceUrl;
  private final String orderServiceUrl;
  private final String shippingServiceUrl;

  @Override
  public InventoryDto updateInventoryStock(int productId, InventoryDto inventory) {
    return null;
  }

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
    return List.of();
  }

  @Override
  public List<OrderDto> getOrders() {
    return List.of();
  }

  @Override
  public List<ProductDto> getProducts() {
    return restTemplate.exchange(productServiceUrl, HttpMethod.GET, null,
      new ParameterizedTypeReference<List<ProductDto>>() {}).getBody();
  }

  @Override
  public List<ProductDto> getProductsByIds(List<Integer> ids) {
    String idsParam = ids.stream()
      .map(String::valueOf)
      .collect(Collectors.joining(","));

    // Add the ids as a query parameter in the URL
    String url = productServiceUrl + "byIds?ids=" + idsParam;

    return restTemplate.exchange(url, HttpMethod.GET, null,
      new ParameterizedTypeReference<List<ProductDto>>() {}).getBody();
  }


  @Override
  public List<ShippingDto> getShipments() {
    return List.of();
  }

  @Override
  public ShippingDto getShipping(int orderId) {
    String url = shippingServiceUrl + orderId;
    return restTemplate.getForObject(url, ShippingDto.class);
  }

  @Override
  public ShippingDto createShippingOrder(ShippingDto shipping) {
    return null;
  }

  @Override
  public ShippingDto updateShippingStatus(int orderId, String status) {
    return null;
  }

  @Override
  public void deleteShipping(int shippingId) {

  }

  @Override
  public ProductDto getProduct(int productId) {
    String url = productServiceUrl + productId;
    return restTemplate.getForObject(url, ProductDto.class);
  }

  @Override
  public ProductDto createProduct(ProductDto product) {
    return null;
  }

  @Override
  public ProductDto updateProduct(int productId, ProductDto product) {
    return null;
  }

  @Override
  public void deleteProduct(int productId) {

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
  public OrderDto createOrder(OrderDto order) {
    return null;
  }

  @Override
  public OrderDto updateOrder(int orderId, OrderDto order) {
    return null;
  }

  @Override
  public void deleteOrder(int orderId) {

  }

  @Override
  public InventoryDto getInventoryStock(int productId) {
    return null;
  }
}
