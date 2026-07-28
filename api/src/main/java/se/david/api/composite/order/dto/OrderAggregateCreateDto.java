package se.david.api.composite.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record OrderAggregateCreateDto(
  @Positive int userId,
  @NotBlank String shippingAddress,
  @NotEmpty @Valid List<OrderItemRequestDto> orderItemCreateDtos) {
}
