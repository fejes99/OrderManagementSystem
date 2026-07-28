package se.david.api.composite.order.dto;

// What a client submits when creating a composite order: no price, since the client
// doesn't (and shouldn't) know a product's price. order-composite-service looks the
// price up and forwards it via api.core.order.dto.OrderItemCreateDto instead.
public record OrderItemRequestDto(int productId, int quantity) {
}
