package se.david.api.core.inventory.dto;

public record InventoryDto(int productId, Integer quantity, String serviceAddress) {
  public InventoryDto {
    if(quantity == null) {
      quantity = 0;
    }
  }

  @Override
  public String toString() {
    return "InventoryDto{" +
      "productId=" + productId +
      ", quantity=" + quantity +
      ", serviceAddress='" + serviceAddress + '\'' +
      '}';
  }
}
