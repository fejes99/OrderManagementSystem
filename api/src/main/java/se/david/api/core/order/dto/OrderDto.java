package se.david.api.core.order.dto;

import java.util.Date;
import java.util.List;

public record OrderDto(int id, int customerId, int totalPrice, String status, Date createdAt,
                       List<OrderItemDto> orderItems, String serviceAddress) {
  @Override
  public String toString() {
    return "OrderDto{" +
      "id=" + id +
      ", customerId=" + customerId +
      ", totalPrice=" + totalPrice +
      ", status='" + status + '\'' +
      ", createdAt=" + createdAt +
      ", orderItems=" + orderItems +
      ", serviceAddress='" + serviceAddress + '\'' +
      '}';
  }
}
