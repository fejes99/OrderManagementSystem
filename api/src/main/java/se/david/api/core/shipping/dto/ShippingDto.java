package se.david.api.core.shipping.dto;

public class ShippingDto {
  private int id;
  private String address;
  private String status;

  public ShippingDto(int id, String address, String status) {
    this.id = id;
    this.address = address;
    this.status = status;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
