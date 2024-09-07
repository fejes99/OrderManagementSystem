package se.david.api.composite.order.dto;

public record OrderItemSummaryDto(int orderItemId, int quantity, int price, ProductSummaryDto product) {
}
