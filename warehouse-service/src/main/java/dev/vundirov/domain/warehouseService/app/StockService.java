package dev.vundirov.domain.warehouseService.app;

import dev.vundirov.common.dto.kafka.OrderCreatedEvent;
import dev.vundirov.domain.warehouseService.entities.IdempotencyKey;
import dev.vundirov.domain.warehouseService.repositories.IdempotencyKeyRepository;
import dev.vundirov.domain.warehouseService.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class StockService {
  private static final Logger logger =
          LoggerFactory.getLogger(StockService.class);


  private final IdempotencyKeyRepository idempotencyKeyRepository;
  private final ProductRepository productRepository;

  @Transactional
  public void processStockReservation(OrderCreatedEvent event) {
    if (idempotencyKeyRepository.existsById(event.messageId())) {
      logger.info("Event {} already processed. Skipping.", event.messageId());
      return;
    }

    idempotencyKeyRepository.save(new IdempotencyKey(event.messageId()));

    for (var item : event.orderItems()) {

      int rowsUpdated =
              productRepository.reserveStock(item.id(), item.quantity());

      if (rowsUpdated == 0) {
        logger.error(
                "Stock mismatch! Product ID: {} is out of stock.", item.id());
        throw new OutOfStockException(
                "Not enough stock for product ID: " + item.id());
      }
    }
  }
}