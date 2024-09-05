package se.david.api.core.order.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class OrderDto {
  private final int id;
  private final int customerId;
  private BigDecimal totalAmount;
  private String status;
  private final Date createdAt = new Date();
  List<OrderItemDto> orderItems;

  public OrderDto(int id, int customerId, BigDecimal totalAmount, String status, List<OrderItemDto> orderItems) {
    this.id = id;
    this.customerId = customerId;
    this.totalAmount = totalAmount;
    this.status = status;
    this.orderItems = orderItems;
  }

  public int getId() {
    return id;
  }

  public int getCustomerId() {
    return customerId;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(BigDecimal totalAmount) {
    this.totalAmount = totalAmount;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public List<OrderItemDto> getOrderItems() {
    return orderItems;
  }

  public void setOrderItems(List<OrderItemDto> orderItems) {
    this.orderItems = orderItems;
  }
}
