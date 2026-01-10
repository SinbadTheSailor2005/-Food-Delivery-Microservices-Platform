package dev.vundirov.domain.warehouseService.app;


import dev.vundirov.app.KafkaConfiguration;
import dev.vundirov.common.dto.kafka.OrderCreatedEvent;
import dev.vundirov.common.dto.kafka.PaymentProcessedEvent;
import dev.vundirov.domain.warehouseService.entities.IdempotencyKey;
import dev.vundirov.domain.warehouseService.repositories.IdempotencyKeyRepository;
import dev.vundirov.domain.warehouseService.repositories.ProductRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.kafka.common.protocol.types.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class WarehouseService {
  private static final Logger logger =
          LoggerFactory.getLogger(WarehouseService.class);

  private StockService stockService;
  private KafkaTemplate<String, Object> kafkaTemplate;




  @KafkaListener(
          topics = KafkaConfiguration.ORDER_CREATED_TOPIC,
          groupId = "warehouse-service-group",
          containerFactory = "objectListenerFactory"


  )
  public void reserveProducts(OrderCreatedEvent event) {
    logger.info("Received OrderCreatedEvent: {}", event);
    try {
    stockService.processStockReservation(event);
    logger.info("Stock reservation succeed for event: {}", event.messageId());
      PaymentProcessedEvent paymentProcessedEvent =
              new PaymentProcessedEvent(
                        event.orderId(),
                      event.messageId(),
                      true,
                        "Stock reserved successfully"
              );
    kafkaTemplate.send(KafkaConfiguration.STOCK_PROCESSED_TOPIC,
            paymentProcessedEvent.orderId().toString(),paymentProcessedEvent);
      logger.info("Sent PaymentProcessedEvent: {}", paymentProcessedEvent);

    } catch (Exception e) {
      logger.warn("Reservation failed for order {}: {}", event.orderId(),
              e.getMessage());
        PaymentProcessedEvent paymentProcessedEvent =
                new PaymentProcessedEvent(event.orderId(),
                        event.messageId(),
                        false,
                        e.getMessage()
                );
      kafkaTemplate.send(KafkaConfiguration.STOCK_PROCESSED_TOPIC,
              paymentProcessedEvent.orderId().toString(),paymentProcessedEvent);
    }
  }
}
