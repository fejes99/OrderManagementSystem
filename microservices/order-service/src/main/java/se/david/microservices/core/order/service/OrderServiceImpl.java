package se.david.microservices.core.order.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import se.david.api.core.order.dto.OrderCreateDto;
import se.david.api.core.order.dto.OrderDto;
import se.david.api.core.order.dto.OrderItemDto;
import se.david.api.core.order.service.OrderService;
import se.david.util.http.ServiceUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
public class OrderServiceImpl implements OrderService {
  private final ServiceUtil serviceUtil;

  @Autowired
  public OrderServiceImpl(ServiceUtil serviceUtil) {
    this.serviceUtil = serviceUtil;
  }

  @Override
  public List<OrderDto> getOrders() {
    return List.of();
  }

  @Override
  public List<OrderDto> getOrdersByUser(int userId) {
    return List.of();
  }

  @Override
  public OrderDto getOrder(int orderId) {
    List<OrderItemDto> orderItems = new ArrayList<>();

    for (int i = 1; i <= 5; i++) {
      orderItems.add(new OrderItemDto(i, orderId, i * 10, i * 2, i * 50));
    }
    return new OrderDto(1, 1, 100, "Created", new Date(), orderItems, serviceUtil.getServiceAddress());
  }

  @Override
  public OrderDto createOrder(OrderCreateDto order) {
    return null;
  }

  @Override
  public OrderDto updateOrder(int orderId, OrderDto order) {
    return null;
  }

  @Override
  public void deleteOrder(int orderId) {

  }
}
