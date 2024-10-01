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
    @Value("${app.product-service.host}") String productServiceHost,
    @Value("${app.product-service.port}") String productServicePort,
    @Value("${app.inventory-service.host}") String inventoryServiceHost,
    @Value("${app.inventory-service.port}") String inventoryServicePort,
    @Value("${app.order-service.host}") String orderServiceHost,
    @Value("${app.order-service.port}") String orderServicePort,
    @Value("${app.shipping-service.host}") String shippingServiceHost,
    @Value("${app.shipping-service.port}") String shippingServicePort) {
    this.restTemplate = restTemplate;
    this.mapper = mapper;

    productServiceUrl = "http://" + productServiceHost + ":" + productServicePort + "/products";
    inventoryServiceUrl = "http://" + inventoryServiceHost + ":" + inventoryServicePort + "/inventories";
    orderServiceUrl = "http://" + orderServiceHost + ":" + orderServicePort + "/orders";
    shippingServiceUrl = "http://" + shippingServiceHost + ":" + shippingServicePort + "/shipments";
  }

  @Override
  public List<InventoryDto> getInventoryStocks() {
    LOG.debug("getInventoryStocks: Fetching inventory stocks from URL: {}", inventoryServiceUrl);
    return restTemplate.exchange(inventoryServiceUrl, HttpMethod.GET, null,
      new ParameterizedTypeReference<List<InventoryDto>>() {}).getBody();
  }

  @Override
  public List<OrderDto> getOrders() {
    LOG.debug("getOrders: Fetching orders from URL: {}", orderServiceUrl);
    return restTemplate.exchange(orderServiceUrl, HttpMethod.GET, null,
      new ParameterizedTypeReference<List<OrderDto>>() {}).getBody();
  }

  @Override
  public List<ProductDto> getProducts() {
    LOG.debug("getProducts: Fetching products from URL: {}", productServiceUrl);
    return restTemplate.exchange(productServiceUrl, HttpMethod.GET, null,
      new ParameterizedTypeReference<List<ProductDto>>() {}).getBody();
  }

  @Override
  public List<ProductDto> getProductsByIds(List<Integer> ids) {
    String idsParam = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    String url = productServiceUrl + "/byIds?ids=" + idsParam;

    LOG.debug("Fetching products by IDs: {} from URL: {}", idsParam, url);
    return restTemplate.exchange(url, HttpMethod.GET, null,
      new ParameterizedTypeReference<List<ProductDto>>() {}).getBody();
  }

  @Override
  public List<ShippingDto> getShipments() {
    LOG.debug("getShipments: Fetching shipments from URL: {}", shippingServiceUrl);
    return restTemplate.exchange(shippingServiceUrl, HttpMethod.GET, null,
      new ParameterizedTypeReference<List<ShippingDto>>() {}).getBody();
  }

  @Override
  public List<ShippingDto> getShipmentsByOrderIds(List<Integer> orderIds) {
    StringBuilder urlBuilder = new StringBuilder(shippingServiceUrl + "/byOrdersIds?");

    for (Integer orderId : orderIds) {
      if (urlBuilder.length() > shippingServiceUrl.length() + "/byOrdersIds?".length()) {
        urlBuilder.append("&");
      }
      urlBuilder.append("orderIds=").append(orderId);
    }

    String url = urlBuilder.toString();

    LOG.debug("getShipmentsByOrderIds: Fetching shipments by order IDs: {} from URL: {}", orderIds, url);
    return restTemplate.exchange(url, HttpMethod.GET, null,
      new ParameterizedTypeReference<List<ShippingDto>>() {}).getBody();
  }


  @Override
  public ShippingDto getShippingByOrderId(int orderId) {
    String url = shippingServiceUrl + "/order/" + orderId;
    LOG.debug("getShippingByOrderId: Fetching shipping details for order ID: {}", orderId);
    return restTemplate.getForObject(url, ShippingDto.class);
  }


  @Override
  public ShippingDto createShippingOrder(ShippingCreateDto shippingCreateDto) {
    LOG.debug("createShippingOrder: Creating new shipping order at URL: {}", shippingServiceUrl);
    ShippingDto shippingDto = restTemplate.postForObject(shippingServiceUrl, shippingCreateDto, ShippingDto.class);
    assert shippingDto != null;
    LOG.debug("createShippingOrder: Created a shipping with orderId: {}", shippingDto.orderId());
    return shippingDto;
  }

  @Override
  public ShippingDto updateShippingStatusByOrderId(int orderId, String status) {
    String url = shippingServiceUrl + "/order/" + orderId;
    LOG.debug("updateShippingStatusByOrderId: Updating shipping status for order ID: {} to status: {}", orderId, status);
    return restTemplate.patchForObject(url, status, ShippingDto.class);
  }

  @Override
  public ProductDto getProduct(int productId) {
    String url = productServiceUrl + "/" + productId;
    LOG.debug("getProduct: Fetching product details for product ID: {}", productId);
    return restTemplate.getForObject(url, ProductDto.class);
  }


  @Override
  public ProductDto createProduct(ProductCreateDto productCreateDto) {
    LOG.debug("createProduct: Creating new product at URL: {}", productServiceUrl);
    ProductDto productDto = restTemplate.postForObject(productServiceUrl, productCreateDto, ProductDto.class);
    assert productDto != null;
    LOG.debug("createProduct: Created a product with id: {}", productDto.id());
    return productDto;
  }

  @Override
  public ProductDto updateProduct(int productId, ProductUpdateDto productUpdateDto) {
    String url = productServiceUrl + "/" + productId;
    LOG.debug("updateProduct: Updating product with ID: {}", productId);
    restTemplate.put(url, productUpdateDto);
    LOG.debug("updateProduct: Updated product with ID: {}", productId);
    return getProduct(productId);
  }



  @Override
  public void deleteProduct(int productId) {
    String url = productServiceUrl + "/" + productId;
    LOG.debug("deleteProduct: Deleting product with ID: {} from URL: {}", productId, url);
    restTemplate.delete(url);
  }

  @Override
  public List<OrderDto> getOrdersByUser(int userId) {
    String url = orderServiceUrl + "/user/" + userId;
    LOG.debug("getOrdersByUser: Fetching orders for user ID: {}", userId);
    return restTemplate.exchange(url, HttpMethod.GET, null,
      new ParameterizedTypeReference<List<OrderDto>>() {}).getBody();
  }

  @Override
  public OrderDto getOrder(int orderId) {
    String url = orderServiceUrl + "/" + orderId;
    LOG.debug("getOrder: Fetching order details for order ID: {}", orderId);
    return restTemplate.getForObject(url, OrderDto.class);
  }

  @Override
  public OrderDto createOrder(OrderCreateDto orderCreateDto) {
    LOG.debug("createOrder: Creating new order at URL: {}", orderServiceUrl);
    OrderDto orderDto = restTemplate.postForObject(orderServiceUrl, orderCreateDto, OrderDto.class);
    assert orderDto != null;
    LOG.debug("createOrder: Created an order with id: {}", orderDto.id());
    return orderDto;
  }

  @Override
  public OrderDto updateOrder(int orderId, OrderUpdateDto orderUpdateDto) {
    String url = orderServiceUrl + "/" + orderId;
    LOG.debug("updateOrder: Updating order with ID: {}", orderId);
    restTemplate.put(url, orderUpdateDto);
    LOG.debug("updateOrder: Updated order with ID: {}", orderId);
    return getOrder(orderId);
  }

  @Override
  public void deleteOrder(int orderId) {
    String url = orderServiceUrl + "/" + orderId;
    LOG.debug("deleteOrder: Deleting order with ID: {} from URL: {}", orderId, url);
    restTemplate.delete(url);
  }

  @Override
  public InventoryDto getInventoryStock(int productId) {
    String url = inventoryServiceUrl + "/" + productId;
    LOG.debug("getInventoryStock: Fetching inventory stock for product ID: {}", productId);
    return restTemplate.getForObject(url, InventoryDto.class);
  }


  @Override
  public InventoryDto createInventoryStock(InventoryCreateDto inventoryCreateDto) {
    LOG.debug("createInventoryStock: Creating inventory stock at URL: {}", inventoryServiceUrl);
    InventoryDto createdInventoryDto = restTemplate.postForObject(inventoryServiceUrl, inventoryCreateDto, InventoryDto.class);
    assert createdInventoryDto != null;
    LOG.debug("createInventoryStock: Created inventory stock with productId: {}", createdInventoryDto.productId());
    return createdInventoryDto;
  }

  @Override
  public void deleteInventoryStock(int productId) {
    String url = inventoryServiceUrl + "/" + productId;
    LOG.debug("deleteInventoryStock: Deleting inventory stock with product ID: {} from URL: {}", productId, url);
    restTemplate.delete(url);
  }

  @Override
  public InventoryDto increaseStock(InventoryStockAdjustmentRequestDto inventoryIncreaseDto) {
    String url = inventoryServiceUrl + "/increaseStock";
    LOG.debug("increaseStock: Increasing stock for the requested product");
    return restTemplate.patchForObject(url, inventoryIncreaseDto, InventoryDto.class);
  }

  @Override
  public void reduceStocks(List<InventoryStockAdjustmentRequestDto> inventoryReduceDtos) {
    String url = inventoryServiceUrl + "/reduceStock";
    LOG.debug("reduceStocks: Reducing stocks for the requested products");
    restTemplate.put(url, inventoryReduceDtos, Void.class);
  }
}
