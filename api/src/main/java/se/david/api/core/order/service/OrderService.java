package se.david.api.core.order.service;

import org.springframework.web.bind.annotation.*;
import se.david.api.core.order.dto.OrderDto;

import java.util.List;

public interface OrderService {
  @GetMapping(
    value = "/orders",
    produces = "application/json")
  List<OrderDto> getOrders();

  @GetMapping(
    value = "/orders/user/{userId}",
    produces = "application/json")
  List<OrderDto> getOrdersByUser(@PathVariable int userId);

  @GetMapping(
    value = "/orders/{orderId}",
    produces = "application/json")
  OrderDto getOrder(@PathVariable int orderId);

  @PostMapping(
    value = "/orders",
    consumes = "application/json",
    produces = "application/json")
  OrderDto createOrder(@RequestBody OrderDto order);

  @PutMapping(
    value = "/orders/{orderId}",
    consumes = "application/json",
    produces = "application/json")
  OrderDto updateOrder(@PathVariable int orderId, @RequestBody OrderDto order);

  @DeleteMapping(
    value = "/orders/{orderId}")
  void deleteOrder(@PathVariable int orderId);
}
