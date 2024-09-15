package se.david.microservices.core.order.domain.entity;

import jakarta.persistence.*;

import java.util.Date;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
  @Id
  @GeneratedValue
  private int id;

  @Version
  private int version;

  @Column(nullable = false)
  private int userId;

  @Column(nullable = false)
  private int totalPrice;

  @Column(nullable = false)
  private String status;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(nullable = false)
  private Date createdAt;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<OrderItem> orderItems;

  public Order() {}

  public Order(int id, int userId, int totalPrice, String status, Date createdAt, List<OrderItem> orderItems) {
    this.id = id;
    this.userId = userId;
    this.totalPrice = totalPrice;
    this.status = status;
    this.createdAt = createdAt;
    this.orderItems = orderItems;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public int getUserId() {
    return userId;
  }

  public void setUserId(int userId) {
    this.userId = userId;
  }

  public int getTotalPrice() {
    return totalPrice;
  }

  public void setTotalPrice(int totalPrice) {
    this.totalPrice = totalPrice;
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

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  public List<OrderItem> getOrderItems() {
    return orderItems;
  }

  public void setOrderItems(List<OrderItem> orderItems) {
    this.orderItems = orderItems;
  }

  @Override
  public String toString() {
    return "Order{" +
      "id=" + id +
      ", version=" + version +
      ", userId=" + userId +
      ", totalPrice=" + totalPrice +
      ", status='" + status + '\'' +
      ", createdAt=" + createdAt +
      ", orderItems=" + orderItems +
      '}';
  }
}
