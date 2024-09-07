package se.david.api.core.shipping.dto;

public record ShippingDto(int orderId, String address, String status, String serviceAddress) {
  @Override
  public String toString() {
    return "ShippingDto{" +
      "orderId=" + orderId +
      ", address='" + address + '\'' +
      ", status='" + status + '\'' +
      ", serviceAddress='" + serviceAddress + '\'' +
      '}';
  }
}
