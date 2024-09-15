package se.david.api.exceptions;

public class InventoryOutOfStockException extends RuntimeException {
  public InventoryOutOfStockException() {
  }

  public InventoryOutOfStockException(String message) {
    super(message);
  }

  public InventoryOutOfStockException(String message, Throwable cause) {
    super(message, cause);
  }

  public InventoryOutOfStockException(Throwable cause) {
    super(cause);
  }
}
