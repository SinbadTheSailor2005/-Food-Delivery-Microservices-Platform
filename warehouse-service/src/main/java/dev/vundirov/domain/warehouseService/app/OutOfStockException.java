package dev.vundirov.domain.warehouseService.app;

public class OutOfStockException extends RuntimeException {
  public OutOfStockException(String message) {
    super(message);
  }
}
