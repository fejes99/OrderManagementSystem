package se.david.api.core.shipping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ShippingCreateDto(
  @NotNull @Positive Integer orderId,
  @NotBlank String shippingAddress) {
}
