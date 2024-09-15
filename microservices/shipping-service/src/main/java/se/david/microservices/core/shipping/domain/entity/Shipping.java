package se.david.microservices.core.shipping.domain.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "shipments")
public class Shipping {
  @Id
  private String id;

  @Version
  private Integer version;

  @Indexed(unique = true)
  private int orderId;
  private String shippingAddress;
  private String status;

  public Shipping() {}

  public Shipping(int orderId, String shippingAddress, String status) {
    this.orderId = orderId;
    this.shippingAddress = shippingAddress;
    this.status = status;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public int getOrderId() {
    return orderId;
  }

  public void setOrderId(int orderId) {
    this.orderId = orderId;
  }

  public String getShippingAddress() {
    return shippingAddress;
  }

  public void setShippingAddress(String shippingAddress) {
    this.shippingAddress = shippingAddress;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return "Shipping{" +
      "id='" + id + '\'' +
      ", version=" + version +
      ", orderId=" + orderId +
      ", shippingAddress='" + shippingAddress + '\'' +
      ", status='" + status + '\'' +
      '}';
  }
}
