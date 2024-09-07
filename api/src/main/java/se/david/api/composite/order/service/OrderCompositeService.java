package se.david.api.composite.order.service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import se.david.api.composite.order.dto.OrderAggregateDto;

import java.util.List;

public interface OrderCompositeService {
  @GetMapping(
    value = "/order-composite",
    produces = "application/json")
  List<OrderAggregateDto> getCompositeOrders();

  @GetMapping(
    value = "/order-composite/user/{userId}",
    produces = "application/json")
  List<OrderAggregateDto> getCompositeOrdersByUser(@PathVariable int userId);

  @GetMapping(
    value = "/order-composite/{orderId}",
    produces = "application/json")
  OrderAggregateDto getCompositeOrder(@PathVariable int orderId);

  @PostMapping(
    value = "/order-composite",
    consumes = "application/json",
    produces = "application/json")
  OrderAggregateDto createCompositeOrder(@RequestBody OrderAggregateDto orderAggregateDto);

}
