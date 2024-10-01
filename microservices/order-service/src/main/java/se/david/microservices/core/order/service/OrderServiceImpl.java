package se.david.microservices.core.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
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
import java.util.stream.Collectors;

@RestController
public class OrderServiceImpl implements OrderService {
  private static final Logger LOG = LoggerFactory.getLogger(OrderServiceImpl.class);
  private final OrderRepository repository;
  private final ServiceUtil serviceUtil;
  private final OrderMapper mapper;
  private final OrderItemMapper itemMapper;

  @Autowired
  public OrderServiceImpl(OrderRepository repository, ServiceUtil serviceUtil, OrderMapper mapper, OrderItemMapper itemMapper) {
    this.repository = repository;
    this.serviceUtil = serviceUtil;
    this.mapper = mapper;
    this.itemMapper = itemMapper;
  }

  @Transactional(readOnly = true)
  @Override
  public List<OrderDto> getOrders() {
    LOG.info("getOrders: Fetching all orders");
    List<Order> orders = (List<Order>) repository.findAll();
    return orders.stream()
      .map(this::mapToOrderDtoWithServiceAddress)
      .collect(Collectors.toList());
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
  public List<OrderDto> getOrdersByUser(int userId) {
    LOG.info("getOrdersByUser: Fetching all orders for userId: {}", userId);
    validateUserId(userId);
    List<Order> orders = repository.findByUserId(userId);
    return orders.stream()
      .map(this::mapToOrderDtoWithServiceAddress)
      .collect(Collectors.toList());
  }

  private void validateUserId(int userId) {
    if (userId < 1) {
      throw new InvalidInputException("Invalid userId: " + userId);
    }
  }

  @Transactional(readOnly = true)
  @Override
  public OrderDto getOrder(int orderId) {
    LOG.debug("getOrder: Search orders for orderId: {}", orderId);
    validateOrderId(orderId);
    Order order = findOrderById(orderId);
    LOG.debug("getOrder: Found order with orderId: {}", orderId);
    return mapper.entityToDto(order);
  }

  private Order findOrderById(int orderId) {
    return repository.findById(orderId)
      .orElseThrow(() -> new NotFoundException("No order found for: " + orderId));
  }

  private void validateOrderId(int orderId) {
    if (orderId < 1) {
      throw new InvalidInputException("Invalid orderId: " + orderId);
    }
  }

  @Transactional
  @Override
  public OrderDto createOrder(OrderCreateDto orderCreateDto) {
    LOG.debug("createOrder: Creating order for userId: {}", orderCreateDto.userId());
    validateUserId(orderCreateDto.userId());

    Order order = mapper.createDtoToEntity(orderCreateDto);

    Order finalOrder = order;
    List<OrderItem> orderItems = orderCreateDto.orderItems().stream()
      .map(itemMapper::createDtoToEntity)
      .peek(orderItem -> orderItem.setOrder(finalOrder))
      .collect(Collectors.toList());

    order.setOrderItems(orderItems);
    order = repository.save(order);

    LOG.debug("createOrder: Successfully created order: {}", order);
    return mapper.entityToDto(order);
  }


  @Override
  public OrderDto updateOrder(int orderId, OrderUpdateDto orderUpdateDto) {
    validateOrderId(orderId);
    LOG.debug("updateOrder: Updating order with id: {}", orderId);

    Order order = findOrderById(orderId);

    mapper.updateEntityToDto(order, orderUpdateDto);
    Order updatedOrder = repository.save(order);

    LOG.debug("updateOrder: Successfully updated order with id: {}", orderId);
    return mapper.entityToDto(updatedOrder);
  }

  @Override
  public void deleteOrder(int orderId) {
    LOG.debug("deleteOrder: Deleting order with id: {}", orderId);

    validateOrderId(orderId);
    Order order = findOrderById(orderId);

    repository.delete(order);
    LOG.debug("deleteOrder: Successfully deleted order with id: {}", orderId);
  }
}
