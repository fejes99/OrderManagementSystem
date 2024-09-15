package se.david.api.composite.order.dto;

import java.util.Date;

public record OrderSummaryDto(int id, int userId, int totalPrice, String status, Date createdAt) {
}
