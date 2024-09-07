package se.david.microservices.composite.order.service.impl;

import io.micrometer.common.KeyValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import se.david.api.composite.order.dto.*;
import se.david.api.composite.order.service.OrderCompositeService;
import se.david.api.core.order.dto.OrderDto;
import se.david.api.core.order.dto.OrderItemDto;
import se.david.api.core.product.dto.ProductDto;
import se.david.api.core.shipping.dto.ShippingDto;
import se.david.microservices.composite.order.service.integration.OrderCompositeIntegration;
import se.david.util.http.ServiceUtil;

import java.util.*;
import java.util.stream.Collectors;

@RestController
public class OrderCompositeServiceImpl implements OrderCompositeService {
  private final ServiceUtil serviceUtil;
  private final OrderCompositeIntegration integration;

  @Autowired
  public OrderCompositeServiceImpl(ServiceUtil serviceUtil, OrderCompositeIntegration integration) {
    this.serviceUtil = serviceUtil;
    this.integration = integration;
  }

  @Override
  public List<OrderAggregateDto> getCompositeOrders() {

    return List.of();
  }

  @Override
  public List<OrderAggregateDto> getCompositeOrdersByUser(int userId) {
    return List.of();
  }

  @Override
  public OrderAggregateDto getCompositeOrder(int orderId) {
    OrderDto order = integration.getOrder(orderId);
    ShippingDto shipping = integration.getShipping(orderId);
    List<OrderItemDto> orderItems = order.orderItems();
    List<ProductDto> products = integration.getProductsByIds(extractUniqueProductIds(orderItems));

    return createOrderAggregateDto(order, shipping, products, orderItems, serviceUtil.getServiceAddress());
  }

  private List<Integer> extractUniqueProductIds(List<OrderItemDto> orderItems) {
    return orderItems.stream()
      .map(OrderItemDto::productId).distinct().collect(Collectors.toList());
  }

  @Override
  public OrderAggregateDto createCompositeOrder(OrderAggregateDto orderAggregateDto) {
    return null;
  }

  private OrderAggregateDto createOrderAggregateDto(
    OrderDto order,
    ShippingDto shipping,
    List<ProductDto> products,
    List<OrderItemDto> orderItems,
    String serviceAddress) {

    // 1. Extract order data
    int orderId = order.id();
    int customerId = order.customerId();
    int totalPrice = order.totalPrice();
    String orderStatus = order.status();
    Date createdAt = order.createdAt();

    // 2. Create shipping summary
    String address = shipping.address();
    String shippingStatus = shipping.status();
    ShippingSummaryDto shippingSummary = new ShippingSummaryDto(orderId, address, shippingStatus);

    // 3. Create a map of productId to ProductDto for easy lookup
    Map<Integer, ProductDto> productMap = products.stream()
      .collect(Collectors.toMap(ProductDto::id, product -> product));

    // 4. Copy order items and map corresponding product details
    List<OrderItemSummaryDto> orderItemSummaries = (orderItems == null) ? null :
      orderItems.stream()
        .map(oi -> {
          ProductDto product = productMap.get(oi.productId());
          ProductSummaryDto productSummary = new ProductSummaryDto(
            product.id(),
            product.name(),
            product.description(),
            product.price());

          // Create OrderItemSummaryDto enriched with product details
          return new OrderItemSummaryDto(
            oi.id(),
            oi.quantity(),
            oi.price(),
            productSummary);
        })
        .toList();

    // 5. Create info regarding the involved microservices addresses
    String productAddress = products.getFirst().serviceAddress();
    String orderAddress = order.serviceAddress();
    String shippingAddress = shipping.serviceAddress();
    ServiceAddressesDto serviceAddresses = new ServiceAddressesDto(serviceAddress, productAddress, orderAddress, shippingAddress);

    return new OrderAggregateDto(orderId, customerId, totalPrice, orderStatus, createdAt, shippingSummary, orderItemSummaries, serviceAddresses);

  }
}
