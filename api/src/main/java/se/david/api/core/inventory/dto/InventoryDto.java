package se.david.api.core.inventory.dto;

public class InventoryDto {
  private final int productId;
  private Integer quantity = 0;

  public InventoryDto(int productId, Integer quantity) {
    this.productId = productId;
    this.quantity = quantity;
  }

  public int getProductId() {
    return productId;
  }

  public Integer getQuantity() {
    return quantity;
  }
}
