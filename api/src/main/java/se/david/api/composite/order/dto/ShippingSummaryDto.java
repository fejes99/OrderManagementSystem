package se.david.api.composite.order.dto;

public record ShippingSummaryDto(int orderId, String address, String status) {
}
