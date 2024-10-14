package se.david.microservices.core.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import se.david.api.core.order.dto.OrderCreateDto;
import se.david.api.core.order.dto.OrderDto;
import se.david.api.core.order.dto.OrderItemDto;
import se.david.api.core.order.dto.OrderUpdateDto;
import se.david.api.core.order.service.OrderService;
import se.david.api.exceptions.InvalidInputException;
import se.david.api.exceptions.NotFoundException;
import se.david.microservices.core.order.domain.entity.Order;
import se.david.microservices.core.order.domain.entity.OrderItem;
import se.david.microservices.core.order.domain.repository.OrderRepository;
import se.david.microservices.core.order.mapper.OrderItemMapper;
import se.david.microservices.core.order.mapper.OrderMapper;
import se.david.util.http.ServiceUtil;

import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

@RestController
public class OrderServiceImpl implements OrderService {
  private static final Logger LOG = LoggerFactory.getLogger(OrderServiceImpl.class);
  private final OrderRepository repository;
  private final ServiceUtil serviceUtil;
  private final OrderMapper mapper;
  private final OrderItemMapper itemMapper;
  private final Scheduler jdbcScheduler;

  @Autowired
  public OrderServiceImpl(@Qualifier("jdbcScheduler") Scheduler jdbcScheduler, OrderRepository repository, ServiceUtil serviceUtil, OrderMapper mapper, OrderItemMapper itemMapper) {
    this.jdbcScheduler = jdbcScheduler;
    this.repository = repository;
    this.serviceUtil = serviceUtil;
    this.mapper = mapper;
    this.itemMapper = itemMapper;
  }

  @Transactional(readOnly = true)
  @Override
  public Flux<OrderDto> getOrders() {
    LOG.info("getOrders: Fetching all orders");

    return Mono.fromCallable(this::internalGetOrders)
      .flatMapMany(Flux::fromIterable)
      .subscribeOn(jdbcScheduler)
      .map(this::mapToOrderDtoWithServiceAddress)
      .doOnError(ex -> LOG.error("Error fetching orders", ex))
      .log(LOG.getName(), Level.FINE);
  }

  private List<Order> internalGetOrders() {
    return (List<Order>) repository.findAll();
  }

  private OrderDto mapToOrderDtoWithServiceAddress(Order order) {
    List<OrderItemDto> orderItemDtos = mapOrderItemsToDtos(order.getOrderItems());
    return new OrderDto(order.getId(), order.getUserId(), order.getTotalPrice(), order.getStatus(), order.getCreatedAt(), orderItemDtos, serviceUtil.getServiceAddress());
  }

  private List<OrderItemDto> mapOrderItemsToDtos(List<OrderItem> orderItems) {
    return orderItems.stream()
      .map(itemMapper::entityToDto)
      .collect(Collectors.toList());
  }

  @Override
  public Flux<OrderDto> getOrdersByUser(int userId) {
    LOG.info("getOrdersByUser: Fetching all orders for userId: {}", userId);
    validateUserId(userId);

    return Mono.fromCallable(() -> findOrdersByUserId(userId))
      .flatMapMany(Flux::fromIterable)
      .subscribeOn(jdbcScheduler)
      .map(this::mapToOrderDtoWithServiceAddress)
      .doOnError(ex -> LOG.error("Error fetching orders for userId: {}", userId, ex))
      .log(LOG.getName(), Level.FINE);
  }

  private List<Order> findOrdersByUserId(int userId) {
    return repository.findByUserId(userId);
  }

  private void validateUserId(int userId) {
    if(userId < 1) {
      throw new InvalidInputException("Invalid userId: " + userId);
    }
  }

  @Transactional(readOnly = true)
  @Override
  public Mono<OrderDto> getOrder(int orderId) {
    LOG.debug("getOrder: Fetching order for orderId: {}", orderId);
    validateOrderId(orderId);

    return Mono.fromCallable(() -> findOrderById(orderId))
      .subscribeOn(jdbcScheduler)
      .map(this::mapToOrderDtoWithServiceAddress)
      .doOnError(ex -> LOG.error("Error fetching order for orderId: {}", orderId, ex))
      .log(LOG.getName(), Level.FINE);
  }

  private Order findOrderById(int orderId) {
    return repository.findById(orderId)
      .orElseThrow(() -> new NotFoundException("Order with id " + orderId + " not found"));
  }

  private void validateOrderId(int orderId) {
    if(orderId < 1) {
      throw new InvalidInputException("Invalid orderId: " + orderId);
    }
  }

  @Transactional
  @Override
  public Mono<OrderDto> createOrder(OrderCreateDto orderCreateDto) {
    LOG.debug("createOrder: Creating order for userId: {}", orderCreateDto.userId());

    validateUserId(orderCreateDto.userId());

    return Mono.fromCallable(() -> internalCreateOrder(orderCreateDto))
      .subscribeOn(jdbcScheduler)
      .map(this::mapToOrderDtoWithServiceAddress)
      .onErrorMap(DuplicateKeyException.class, ex ->
        new InvalidInputException("Duplicate order for userId: " + orderCreateDto.userId()))
      .doOnSuccess(savedOrder -> LOG.debug("Successfully created order with id: {}", savedOrder.id()))
      .doOnError(ex -> LOG.error("Error creating order for userId: {}", orderCreateDto.userId(), ex))
      .log(LOG.getName(), Level.FINE);
  }

  private Order internalCreateOrder(OrderCreateDto orderCreateDto) {
    Order order = mapper.createDtoToEntity(orderCreateDto);

    List<OrderItem> orderItems = orderCreateDto.orderItems().stream()
      .map(itemMapper::createDtoToEntity)
      .peek(orderItem -> orderItem.setOrder(order))
      .collect(Collectors.toList());

    order.setOrderItems(orderItems);

    return repository.save(order);
  }


  @Override
  public Mono<OrderDto> updateOrder(int orderId, OrderUpdateDto orderUpdateDto) {
    LOG.debug("updateOrder: Updating order with id: {}", orderId);

    validateOrderId(orderId);

    return Mono.fromCallable(() -> internalUpdateOrder(orderId, orderUpdateDto))
      .subscribeOn(jdbcScheduler)
      .map(this::mapToOrderDtoWithServiceAddress)
      .onErrorMap(IllegalArgumentException.class, ex ->
        new InvalidInputException("Invalid orderId: " + orderId))
      .doOnSuccess(updatedOrder -> LOG.debug("Successfully updated order with id: {}", updatedOrder.id()))
      .doOnError(ex -> LOG.error("Error updating order with id: {}", orderId, ex))
      .log(LOG.getName(), Level.FINE);
  }


  private Order internalUpdateOrder(int orderId, OrderUpdateDto orderUpdateDto) {
    Order order = findOrderById(orderId);

    mapper.updateEntityToDto(order, orderUpdateDto);

    return repository.save(order);
  }

  @Override
  public Mono<Void> deleteOrder(int orderId) {
    LOG.debug("deleteOrder: Deleting order with id: {}", orderId);
    validateOrderId(orderId);

    return Mono.fromRunnable(() -> internalDeleteOrder(orderId))
      .subscribeOn(jdbcScheduler)
      .doOnError(ex -> LOG.error("Error deleting order with id: {}", orderId, ex))
      .then();
  }

  private void internalDeleteOrder(int orderId) {
    Order order = findOrderById(orderId);
    repository.delete(order);
  }
}
