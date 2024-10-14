package se.david.microservices.composite.order.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.david.api.composite.order.dto.*;
import se.david.api.composite.order.service.OrderCompositeService;
import se.david.api.core.order.dto.OrderCreateDto;
import se.david.api.core.order.dto.OrderDto;
import se.david.api.core.order.dto.OrderItemDto;
import se.david.api.core.product.dto.ProductDto;
import se.david.api.core.shipping.dto.ShippingCreateDto;
import se.david.api.core.shipping.dto.ShippingDto;
import se.david.microservices.composite.order.service.integration.OrderCompositeIntegration;
import se.david.util.http.ServiceUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class OrderCompositeServiceImpl implements OrderCompositeService {
  private static final Logger LOG = LoggerFactory.getLogger(OrderCompositeServiceImpl.class);
  private final SecurityContext nullSecCtx = new SecurityContextImpl();

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

    return getLogAuthorizationInfoMono()
      .thenMany(integration.getOrders())
      .flatMap(this::buildOrderAggregate)
      .doOnError(ex -> LOG.error("Error retrieving composite orders: {}", ex.toString()))
      .onErrorResume(e -> Flux.empty());
  }

  private Mono<OrderAggregateDto> buildOrderAggregate(OrderDto order) {
    return Mono.zip(
        Mono.just(order),
        getShippingForOrder(order.id()),
        getProductsForOrder(order.orderItems())
      )
      .map(tuple -> createOrderAggregateDto(
        tuple.getT1(), // order
        tuple.getT2(), // shipping
        tuple.getT3(), // products
        order.orderItems(),
        serviceUtil.getServiceAddress()
      ))
      .doOnSuccess(agg -> LOG.debug("buildOrderAggregate: Created order aggregate DTO for orderId: {}", order.id()))
      .doOnError(ex -> LOG.error("Error building order aggregate for orderId: {}, error: {}", order.id(), ex.toString()));
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
    LOG.debug("getCompositeOrdersByUser: Starting to retrieve order aggregates for userId: {}", userId);

    return getLogAuthorizationInfoMono()
      .thenMany(integration.getOrdersByUser(userId))
      .flatMap(this::buildOrderAggregate)
      .doOnError(ex -> LOG.error("Error retrieving composite orders for userId: {}, error: {}", userId, ex.toString()))
      .onErrorResume(e -> Flux.empty());
  }

  @Override
  public Mono<OrderAggregateDto> getCompositeOrder(int orderId) {
    LOG.debug("getCompositeOrder: Starting to retrieve order for orderId: {}", orderId);

    return getLogAuthorizationInfoMono()
      .then(integration.getOrder(orderId))
      .flatMap(this::buildOrderAggregate)
      .doOnError(ex -> LOG.error("Error retrieving composite order for orderId: {}, error: {}", orderId, ex.toString()))
      .onErrorResume(e -> Mono.empty());
  }

  @Override
  public Mono<OrderAggregateDto> createCompositeOrder(OrderAggregateCreateDto orderAggregateCreateDto) {
    LOG.debug("createCompositeOrder: Starting to create composite order for userId: {}", orderAggregateCreateDto.userId());

    return getLogAuthorizationInfoMono()
      .then(getProductsForOrderItems(orderAggregateCreateDto.orderItemCreateDtos()))
      .flatMap(products -> createOrderAndShipping(orderAggregateCreateDto, products))
      .doOnSuccess(orderAggregateDto -> LOG.info("Successfully created composite order for userId: {}, orderId: {}",
        orderAggregateCreateDto.userId(), orderAggregateDto.orderId()))
      .doOnError(ex -> LOG.error("Failed to create composite order for userId: {}, error: {}",
        orderAggregateCreateDto.userId(), ex.toString()))
      .onErrorResume(this::handleOrderCreationError);
  }

  private Mono<OrderAggregateDto> createOrderAndShipping(OrderAggregateCreateDto orderCreateDto, List<ProductDto> products) {
    return createOrder(orderCreateDto)
      .flatMap(order -> createShipping(order, orderCreateDto)
        .map(shipping -> buildOrderAggregateDto(order, shipping, products, orderCreateDto))
      )
      .doOnError(ex -> LOG.error("Error creating order or shipping for userId: {}, error: {}",
        orderCreateDto.userId(), ex.toString()));
  }

  private OrderAggregateDto buildOrderAggregateDto(OrderDto order, ShippingDto shipping, List<ProductDto> products, OrderAggregateCreateDto orderCreateDto) {
    LOG.debug("Building OrderAggregateDto for orderId: {}, userId: {}", order.id(), orderCreateDto.userId());

    return createOrderAggregateDto(order, shipping, products, order.orderItems(), serviceUtil.getServiceAddress());
  }

  private Mono<OrderDto> createOrder(OrderAggregateCreateDto orderAggregateCreateDto) {
    LOG.debug("createOrder: Creating order for userId {}", orderAggregateCreateDto.userId());
    OrderCreateDto orderCreateDto = new OrderCreateDto(orderAggregateCreateDto.userId(), orderAggregateCreateDto.orderItemCreateDtos());
    return integration.createOrder(orderCreateDto)
      .doOnError(e -> LOG.error("Error creating order for userId {}", orderAggregateCreateDto.userId(), e));
  }

  private Mono<ShippingDto> createShipping(OrderDto order, OrderAggregateCreateDto orderAggregateCreateDto) {
    LOG.debug("createShipping: Creating shipping for orderId {}", order.id());
    ShippingCreateDto shippingCreateDto = new ShippingCreateDto(order.id(), orderAggregateCreateDto.shippingAddress());
    return integration.createShippingOrder(shippingCreateDto)
      .doOnError(e -> LOG.error("Error creating shipping for orderId {}", order.id(), e));
  }

  private Mono<OrderAggregateDto> handleOrderCreationError(Throwable e) {
    LOG.error("Error creating composite order", e);
    return Mono.empty();
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
    int totalPrice = orderItem.quantity() * product.price();

    return new OrderItemSummaryDto(
      orderItem.id(),
      orderItem.quantity(),
      totalPrice,
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

  private Mono<SecurityContext> getLogAuthorizationInfoMono() {
    return getSecurityContextMono().doOnNext(this::logAuthorizationInfo);
  }

  private Mono<SecurityContext> getSecurityContextMono() {
    return ReactiveSecurityContextHolder.getContext().defaultIfEmpty(nullSecCtx);
  }

  private void logAuthorizationInfo(SecurityContext sc) {
    if(sc != null && sc.getAuthentication() != null && sc.getAuthentication() instanceof JwtAuthenticationToken) {
      Jwt jwtToken = ((JwtAuthenticationToken) sc.getAuthentication()).getToken();
      logAuthorizationInfo(jwtToken);
    } else {
      LOG.warn("No JWT based Authentication supplied.");
    }
  }

  private void logAuthorizationInfo(Jwt jwt) {
    if(jwt == null) {
      LOG.warn("No JWT supplied.");
    } else {
      LOG.debug("Authorization info: Subject: {}, scopes: {}, expires {}, issuer: {}, audience: {}",
        jwt.getClaims().get("sub"),
        jwt.getClaims().get("scope"),
        jwt.getClaims().get("exp"),
        jwt.getIssuer(),
        jwt.getAudience()
      );
    }
  }

}
