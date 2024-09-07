package se.david.api.core.product.dto;

public record ProductDto(int id, String name, String description, int price, String serviceAddress) {
  @Override
  public String toString() {
    return "ProductDto{" +
      "id=" + id +
      ", name='" + name + '\'' +
      ", description='" + description + '\'' +
      ", price=" + price +
      ", serviceAddress='" + serviceAddress + '\'' +
      '}';
  }
}
