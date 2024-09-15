package se.david.microservices.core.inventory.domain.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "inventories")
public class Inventory {
  @Id
  private String id;

  @Version
  private Integer version;

  @Indexed(unique = true)
  private int productId;
  private int quantity;

  public Inventory() {}

  public Inventory(int productId, int quantity) {
    this.productId = productId;
    this.quantity = quantity;
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

  public int getProductId() {
    return productId;
  }

  public void setProductId(int productId) {
    this.productId = productId;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  @Override
  public String toString() {
    return "Inventory{" +
      "id='" + id + '\'' +
      ", version=" + version +
      ", productId=" + productId +
      ", quantity=" + quantity +
      '}';
  }
}
