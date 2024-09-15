package se.david.api.composite.order.dto;

import java.util.List;

public record OrderAggregateCreateDto(
  int userId,
  String shippingAddress,
  List<OrderItemCreateDto> orderItemCreateDtos) {
}
