package se.david.api.core.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record OrderCreateDto(
  @Positive int userId,
  @NotEmpty @Valid List<OrderItemCreateDto> orderItems) {
}
