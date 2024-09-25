package se.david.api.core.shipping.dto;

public record ShippingCreateDto(Integer orderId, String shippingAddress) {
}
