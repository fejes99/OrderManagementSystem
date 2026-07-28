package se.david.api.core.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record InventoryCreateDto(
  @Positive int productId,
  @NotNull @PositiveOrZero Integer quantity) {
}
