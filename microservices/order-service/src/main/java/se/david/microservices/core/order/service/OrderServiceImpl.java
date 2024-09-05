package se.david.microservices.core.order.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import se.david.api.core.order.dto.OrderDto;
import se.david.api.core.order.service.OrderService;
import se.david.util.http.ServiceUtil;

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
    return null;
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
}
