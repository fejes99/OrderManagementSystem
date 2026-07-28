package se.david.api.core.inventory.dto;

import jakarta.validation.constraints.Positive;

public record InventoryStockAdjustmentRequestDto(
  @Positive int productId,
  @Positive int quantity) {
}
