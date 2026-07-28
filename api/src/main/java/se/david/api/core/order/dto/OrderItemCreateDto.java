package se.david.api.core.order.dto;

public record OrderItemCreateDto(int productId, int quantity, int price) {
}
