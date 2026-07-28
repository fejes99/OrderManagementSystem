package se.david.api.core.order.dto;

// price is the product's unit price at order-creation time, not a line total.
// order-composite-service looks this up from product-service and populates it here,
// since order-service itself never calls product-service.
public record OrderItemCreateDto(int productId, int quantity, int price) {
}
