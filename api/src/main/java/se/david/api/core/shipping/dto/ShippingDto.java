package se.david.api.core.shipping.dto;

public record ShippingDto(int orderId, String shippingAddress, String status, String serviceAddress) {
  @Override
  public String toString() {
    return "ShippingDto{" +
      "orderId=" + orderId +
      ", shippingAddress='" + shippingAddress + '\'' +
      ", status='" + status + '\'' +
      ", serviceAddress='" + serviceAddress + '\'' +
      '}';
  }
}
