package se.david.microservices.composite.order.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import se.david.api.composite.order.dto.*;
import se.david.api.composite.order.service.OrderCompositeService;
import se.david.api.core.inventory.dto.InventoryCheckRequestDto;
import se.david.api.core.inventory.dto.InventoryReduceRequestDto;
import se.david.api.core.order.dto.OrderCreateDto;
import se.david.api.core.order.dto.OrderDto;
import se.david.api.core.order.dto.OrderItemDto;
import se.david.api.core.product.dto.ProductDto;
import se.david.api.core.shipping.dto.ShippingCreateDto;
import se.david.api.core.shipping.dto.ShippingDto;
import se.david.microservices.composite.order.service.integration.OrderCompositeIntegration;
import se.david.util.http.ServiceUtil;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
public class OrderCompositeServiceImpl implements OrderCompositeService {
  private static final Logger LOG = LoggerFactory.getLogger(OrderCompositeServiceImpl.class);

  private final ServiceUtil serviceUtil;
  private final OrderCompositeIntegration integration;

  @Autowired
  public OrderCompositeServiceImpl(ServiceUtil serviceUtil, OrderCompositeIntegration integration) {
    this.serviceUtil = serviceUtil;
    this.integration = integration;
  }

  @Override
  public List<OrderAggregateDto> getCompositeOrders() {
    LOG.debug("getCompositeOrders: Starting to retrieve order aggregates.");

    List<OrderDto> orders = integration.getOrders();
    LOG.debug("getCompositeOrders: Retrieved {} orders.", orders.size());

    Map<Integer, ShippingDto> shipmentMap = getShipmentsMappedByOrderId(orders);
    LOG.debug("getCompositeOrders: Mapped {} shipments to orders.", shipmentMap.size());

    Map<Integer, ProductDto> productMap = getProductMapFromOrders(orders);
    LOG.debug("getCompositeOrders: Mapped {} products from orders.", productMap.size());

    List<OrderAggregateDto> orderAggregates = orders.stream()
      .map(order -> {
        ShippingDto shipping = shipmentMap.get(order.id());
        return createOrderAggregateDto(order, shipping, new ArrayList<>(productMap.values()), order.orderItems(), serviceUtil.getServiceAddress());
      })
      .collect(Collectors.toList());
    LOG.debug("getCompositeOrders: Created {} order aggregate DTOs.", orderAggregates.size());

    return orderAggregates;
  }

  private Map<Integer, ShippingDto> getShipmentsMappedByOrderId(List<OrderDto> orders) {
    Set<Integer> orderIds = extractOrderIds(orders);
    LOG.debug("getShipmentsMappedByOrderId: Retrieving shipments for {} orders.", orderIds.size());

    List<ShippingDto> shipments = integration.getShipmentsByIds(new ArrayList<>(orderIds));
    LOG.debug("getShipmentsMappedByOrderId: Retrieved {} shipments.", shipments.size());

    return shipments.stream()
      .collect(Collectors.toMap(ShippingDto::orderId, shippingDto -> shippingDto));
  }

  private Set<Integer> extractOrderIds(List<OrderDto> orders) {
    return orders.stream()
      .map(OrderDto::id)
      .collect(Collectors.toSet());
  }

  private Map<Integer, ProductDto> getProductMapFromOrders(List<OrderDto> orders) {
    List<OrderItemDto> orderItems = collectAllOrderItems(orders);
    List<Integer> productIds = extractUniqueProductIds(orderItems, OrderItemDto::productId);
    LOG.debug("getProductMapFromOrders: Retrieving products for {} unique product IDs.", productIds.size());

    List<ProductDto> products = integration.getProductsByIds(productIds);
    LOG.debug("getProductMapFromOrders: Retrieved {} products.", products.size());

    return products.stream()
      .collect(Collectors.toMap(ProductDto::id, product -> product));
  }

  private <T> List<Integer> extractUniqueProductIds(List<T> orderItems, Function<T, Integer> productIdMapper) {
    return orderItems.stream()
      .map(productIdMapper)
      .distinct()
      .collect(Collectors.toList());
  }


  private List<OrderItemDto> collectAllOrderItems(List<OrderDto> orders) {
    return orders.stream()
      .flatMap(order -> order.orderItems().stream())
      .collect(Collectors.toList());
  }

  @Override
  public List<OrderAggregateDto> getCompositeOrdersByUser(int userId) {
    LOG.debug("getCompositeOrdersByUser: Starting to retrieve order aggregates for userId {}.", userId);

    List<OrderDto> orders = integration.getOrdersByUser(userId);
    LOG.debug("getCompositeOrdersByUser: Retrieved {} orders for userId {}.", orders.size(), userId);

    if (orders.isEmpty()) {
      LOG.info("getCompositeOrdersByUser: No orders found for userId {}.", userId);
      return Collections.emptyList();
    }

    Map<Integer, ShippingDto> shipmentMap = getShipmentsMappedByOrderId(orders);
    LOG.debug("getCompositeOrdersByUser: Mapped {} shipments to orders.", shipmentMap.size());

    Map<Integer, ProductDto> productMap = getProductMapFromOrders(orders);
    LOG.debug("getCompositeOrdersByUser: Mapped {} products from orders.", productMap.size());

    List<OrderAggregateDto> orderAggregates = orders.stream()
      .map(order -> {
        ShippingDto shipping = shipmentMap.get(order.id());
        return createOrderAggregateDto(order, shipping, new ArrayList<>(productMap.values()), order.orderItems(), serviceUtil.getServiceAddress());
      })
      .collect(Collectors.toList());
    LOG.debug("getCompositeOrdersByUser: Created {} order aggregate DTOs for userId {}.", orderAggregates.size(), userId);

    return orderAggregates;
  }

  @Override
  public OrderAggregateDto getCompositeOrder(int orderId) {
    LOG.debug("getCompositeOrder: Starting to retrieve order for orderId {}.", orderId);

    OrderDto order = integration.getOrder(orderId);
    if (order == null) {
      LOG.warn("getCompositeOrder: No order found for orderId {}.", orderId);
      return null; // todo: implement bad request exception
    }
    LOG.debug("getCompositeOrder: Successfully retrieved order with orderId {}.", orderId);

    ShippingDto shipping = integration.getShipping(orderId);
    if (shipping == null) {
      LOG.warn("getCompositeOrder: No shipping details found for orderId {}.", orderId);
      return null;
    }
    LOG.debug("getCompositeOrder: Successfully retrieved shipping details for orderId {}.", orderId);

    List<OrderItemDto> orderItems = order.orderItems();
    List<ProductDto> products = integration.getProductsByIds(extractUniqueProductIds(orderItems, OrderItemDto::productId));
    LOG.debug("getCompositeOrder: Extracted {} unique products from order items for orderId {}.", products.size(), orderId);

    OrderAggregateDto orderAggregate = createOrderAggregateDto(order, shipping, products, orderItems, serviceUtil.getServiceAddress());
    LOG.debug("getCompositeOrder: Created OrderAggregateDto for orderId {}.", orderId);

    return orderAggregate;
  }

  // todo: implement saga pattern
  @Override
  public OrderAggregateDto createCompositeOrder(OrderAggregateCreateDto orderAggregateCreateDto) throws Exception {
    LOG.debug("createCompositeOrder: Starting to creating order");

    List<OrderItemCreateDto> orderItemCreateDtos = orderAggregateCreateDto.orderItemCreateDtos();
    List<Integer> productIds = extractUniqueProductIds(orderItemCreateDtos, OrderItemCreateDto::productId);

    List<ProductDto> products = integration.getProductsByIds(productIds);

    List<InventoryCheckRequestDto> inventoryCheckRequests = orderItemCreateDtos
      .stream()
      .map(oi -> new InventoryCheckRequestDto(oi.productId(), oi.quantity()))
      .collect(Collectors.toList());

    boolean isStockAvailable = integration.checkStock(inventoryCheckRequests);
    if (!isStockAvailable) {
      throw new Exception("One or more items are out of stock.");
    }

    List<InventoryReduceRequestDto> inventoryReduceRequests = inventoryCheckRequests
      .stream()
      .map(oi -> new InventoryReduceRequestDto(oi.productId(), oi.quantity()))
      .collect(Collectors.toList());

    integration.reduceStock(inventoryReduceRequests);

    OrderCreateDto orderCreateDto = new OrderCreateDto(
      orderAggregateCreateDto.userId(),
      orderAggregateCreateDto.orderItemCreateDtos()
    );

    OrderDto createdOrder = integration.createOrder(orderCreateDto);

    ShippingCreateDto shippingCreateDto = new ShippingCreateDto(orderAggregateCreateDto.shippingAddress());
    ShippingDto createdShipping = integration.createShippingOrder(shippingCreateDto);

    OrderAggregateDto createdOrderAggregate = createOrderAggregateDto(
     createdOrder, createdShipping, products, createdOrder.orderItems(), serviceUtil.getServiceAddress()
    );
    LOG.debug("createCompositeOrder: Created OrderAggregateDto for orderId {}.", createdOrder.id());

    return createdOrderAggregate;
  }

  private OrderAggregateDto createOrderAggregateDto(
    OrderDto order,
    ShippingDto shipping,
    List<ProductDto> products,
    List<OrderItemDto> orderItems,
    String serviceAddress) {
    OrderSummaryDto orderSummary = createOrderSummary(order);

    ShippingSummaryDto shippingSummary = createShippingSummary(shipping);

    Map<Integer, ProductDto> productMap = createProductMap(products);

    List<OrderItemSummaryDto> orderItemSummaries = createOrderItemSummaries(orderItems, productMap);

    ServiceAddressesDto serviceAddresses = createServiceAddressesDto(
      serviceAddress,
      products,
      order,
      shipping
    );

    return new OrderAggregateDto(
      orderSummary.id(),
      orderSummary.userId(),
      orderSummary.totalPrice(),
      orderSummary.status(),
      orderSummary.createdAt(),
      shippingSummary,
      orderItemSummaries,
      serviceAddresses
    );
  }

  private OrderSummaryDto createOrderSummary(OrderDto order) {
    return new OrderSummaryDto(
      order.id(),
      order.userId(),
      order.totalPrice(),
      order.status(),
      order.createdAt()
    );
  }

  private ShippingSummaryDto createShippingSummary(ShippingDto shipping) {
    return new ShippingSummaryDto(
      shipping.orderId(),
      shipping.address(),
      shipping.status()
    );
  }

  private Map<Integer, ProductDto> createProductMap(List<ProductDto> products) {
    return products.stream()
      .collect(Collectors.toMap(ProductDto::id, product -> product));
  }

  private List<OrderItemSummaryDto> createOrderItemSummaries(
    List<OrderItemDto> orderItems,
    Map<Integer, ProductDto> productMap
  ) {
    return (orderItems == null) ? Collections.emptyList() :
      orderItems.stream()
        .map(oi -> createOrderItemSummary(oi, productMap))
        .collect(Collectors.toList());
  }

  private OrderItemSummaryDto createOrderItemSummary(
    OrderItemDto orderItem,
    Map<Integer, ProductDto> productMap
  ) {
    ProductDto product = productMap.get(orderItem.productId());
    ProductSummaryDto productSummary = createProductSummary(product);

    return new OrderItemSummaryDto(
      orderItem.id(),
      orderItem.quantity(),
      orderItem.price(),
      productSummary
    );
  }

  private ProductSummaryDto createProductSummary(ProductDto product) {
    return new ProductSummaryDto(
      product.id(),
      product.name(),
      product.description(),
      product.price()
    );
  }

  private ServiceAddressesDto createServiceAddressesDto(
    String serviceAddress,
    List<ProductDto> products,
    OrderDto order,
    ShippingDto shipping
  ) {
    String productAddress = !products.isEmpty() ? products.getFirst().serviceAddress() : "";
    return new ServiceAddressesDto(
      serviceAddress,
      productAddress,
      order.serviceAddress(),
      shipping.serviceAddress()
    );
  }
}
