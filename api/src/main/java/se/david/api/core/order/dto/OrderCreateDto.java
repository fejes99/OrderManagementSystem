package se.david.api.core.order.dto;

import se.david.api.composite.order.dto.OrderItemCreateDto;

import java.util.List;

public record OrderCreateDto(int userId, List<OrderItemCreateDto> orderItems) {
}
