package se.david.api.core.order.dto;

public record OrderItemDto(int id, int orderId, int productId, int quantity, int price) {
  @Override
  public String toString() {
    return "OrderItemDto{" +
      "id=" + id +
      ", orderId=" + orderId +
      ", productId=" + productId +
      ", quantity=" + quantity +
      ", price=" + price +
      '}';
  }
}
