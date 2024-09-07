package se.david.api.composite.order.dto;

public record ServiceAddressesDto(
  String compositeAddress,
  String productAddress,
  String orderAddress,
  String shippingAddress) {
}
