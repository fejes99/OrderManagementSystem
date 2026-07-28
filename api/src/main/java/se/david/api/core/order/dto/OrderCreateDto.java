package se.david.api.core.order.dto;

import java.util.List;

public record OrderCreateDto(int userId, List<OrderItemCreateDto> orderItems) {
}
