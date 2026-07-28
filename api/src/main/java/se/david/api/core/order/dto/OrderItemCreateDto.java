package se.david.api.core.order.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record OrderItemCreateDto(
  @Positive int productId,
  @Positive int quantity,
  @PositiveOrZero int price) {
}
