package se.david.api.composite.order.dto;

import jakarta.validation.constraints.Positive;

public record OrderItemRequestDto(
  @Positive int productId,
  @Positive int quantity) {
}
