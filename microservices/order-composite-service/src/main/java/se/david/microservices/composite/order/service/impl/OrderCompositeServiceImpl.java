package se.david.microservices.composite.order.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.david.api.composite.order.dto.*;
import se.david.api.composite.order.service.OrderCompositeService;
import se.david.api.core.inventory.dto.InventoryStockAdjustmentRequestDto;
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
  public Flux<OrderAggregateDto> getCompositeOrders() {
    LOG.debug("getCompositeOrders: Starting to retrieve order aggregates.");

    return integration.getOrders()
      .flatMap(order -> Mono.zip(
            Mono.just(order),
            getShippingForOrder(order.id()),
            getProductsForOrder(order.orderItems())
          )
          .map(tuple -> createOrderAggregateDto(tuple.getT1(), tuple.getT2(), tuple.getT3(), order.orderItems(), serviceUtil.getServiceAddress()))
          .doOnSuccess(agg -> LOG.debug("getCompositeOrders: Created order aggregate DTO for orderId: {}", order.id()))
      )
      .onErrorResume(e -> {
        LOG.error("Error retrieving composite orders", e);
        return Flux.empty();
      });
  }

  private Mono<ShippingDto> getShippingForOrder(int orderId) {
    LOG.debug("getShippingForOrder: Retrieving shipping for orderId {}", orderId);
    return integration.getShippingByOrderId(orderId)
      .doOnError(e -> LOG.error("Error retrieving shipping for orderId: {}", orderId, e));
  }

  private Mono<List<ProductDto>> getProductsForOrder(List<OrderItemDto> orderItems) {
    List<Integer> productIds = orderItems.stream()
      .map(OrderItemDto::productId)
      .distinct()
      .collect(Collectors.toList());
    LOG.debug("getProductsForOrder: Retrieving products for productIds: {}", productIds);

    return integration.getProductsByIds(productIds)
      .collectList()
      .doOnError(e -> LOG.error("Error retrieving products for order", e));
  }

  @Override
  public Flux<OrderAggregateDto> getCompositeOrdersByUser(int userId) {
    LOG.debug("getCompositeOrdersByUser: Starting to retrieve order aggregates for userId {}.", userId);

    return integration.getOrdersByUser(userId)
      .flatMap(order -> Mono.zip(
            Mono.just(order),
            getShippingForOrder(order.id()),
            getProductsForOrder(order.orderItems())
          )
          .map(tuple -> createOrderAggregateDto(tuple.getT1(), tuple.getT2(), tuple.getT3(), order.orderItems(), serviceUtil.getServiceAddress()))
          .doOnSuccess(agg -> LOG.debug("getCompositeOrdersByUser: Created order aggregate DTO for orderId: {}", order.id()))
      )
      .onErrorResume(e -> {
        LOG.error("Error retrieving composite orders for userId: {}", userId, e);
        return Flux.empty();
      });
  }

  @Override
  public Mono<OrderAggregateDto> getCompositeOrder(int orderId) {
    LOG.debug("getCompositeOrder: Starting to retrieve order for orderId {}.", orderId);

    return integration.getOrder(orderId)
      .flatMap(order -> Mono.zip(
            Mono.just(order),
            getShippingForOrder(orderId),
            getProductsForOrder(order.orderItems())
          )
          .map(tuple -> createOrderAggregateDto(tuple.getT1(), tuple.getT2(), tuple.getT3(), order.orderItems(), serviceUtil.getServiceAddress()))
          .doOnSuccess(agg -> LOG.debug("getCompositeOrder: Created OrderAggregateDto for orderId: {}", orderId))
      )
      .onErrorResume(e -> {
        LOG.error("Error retrieving composite order for orderId: {}", orderId, e);
        return Mono.empty();
      });
  }

  @Override
  public Mono<OrderAggregateDto> createCompositeOrder(OrderAggregateCreateDto orderAggregateCreateDto) {
    LOG.debug("createCompositeOrder: Starting to create composite order");

    return getProductsForOrderItems(orderAggregateCreateDto.orderItemCreateDtos())
      .flatMap(products ->
         integration.createOrder(new OrderCreateDto(orderAggregateCreateDto.userId(), orderAggregateCreateDto.orderItemCreateDtos()))
          .flatMap(createdOrder -> integration.createShippingOrder(new ShippingCreateDto(createdOrder.id(), orderAggregateCreateDto.shippingAddress()))
            .map(createdShipping -> createOrderAggregateDto(createdOrder, createdShipping, products, createdOrder.orderItems(), serviceUtil.getServiceAddress()))
          ))
      .doOnSuccess(agg -> LOG.debug("createCompositeOrder: Successfully created composite order with orderId: {}", agg.orderId()))
      .onErrorResume(e -> {
        LOG.error("Error creating composite order", e);
        return Mono.empty();
      });
  }

  private Mono<List<ProductDto>> getProductsForOrderItems(List<OrderItemCreateDto> orderItems) {
    List<Integer> productIds = orderItems.stream()
      .map(OrderItemCreateDto::productId)
      .distinct()
      .collect(Collectors.toList());
    LOG.debug("getProductsForOrderItems: Retrieving products for productIds: {}", productIds);

    return integration.getProductsByIds(productIds)
      .collectList()
      .doOnError(e -> LOG.error("Error retrieving products for order items", e));
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
      shipping.shippingAddress(),
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
