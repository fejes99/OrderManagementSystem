package se.david.api.composite.order.dto;

// price is the product's unit price at order-creation time, not a line total.
public record OrderItemCreateDto(int productId, int quantity, int price) {
}