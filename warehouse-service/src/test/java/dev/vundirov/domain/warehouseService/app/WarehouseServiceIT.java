package dev.vundirov.domain.warehouseService.app;


import dev.vundirov.app.KafkaConfiguration;
import dev.vundirov.common.dto.OrderItemDto;
import dev.vundirov.common.dto.kafka.OrderCreatedEvent;
import dev.vundirov.common.dto.kafka.PaymentProcessedEvent;
import dev.vundirov.common.dto.kafka.StockProcessedEvent;
import dev.vundirov.domain.warehouseService.entities.Product;
import dev.vundirov.domain.warehouseService.repositories.IdempotencyKeyRepository;
import dev.vundirov.domain.warehouseService.repositories.ProductRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class WarehouseServiceIT extends AbstractIntegrationTest {

  @Autowired
  private ProductRepository productRepository;
  @Autowired
  private IdempotencyKeyRepository idempotencyKeyRepository;
  @Autowired
  private KafkaTemplate<String, Object> kafkaTemplate;

  private Consumer<String, StockProcessedEvent> testConsumer;


  @BeforeEach
  void setUp() {
    idempotencyKeyRepository.deleteAll();
    productRepository.deleteAll();

    Map<String, Object> props =
            KafkaTestUtils.consumerProps(
                    kafka.getBootstrapServers(), "test" +
                            "-warehouse-group", "true"
            );
    JsonDeserializer<StockProcessedEvent> jsonDeserializer =
            new JsonDeserializer<>(StockProcessedEvent.class, false);
    jsonDeserializer.addTrustedPackages("dev.vundirov" +
            ".common.dto.kafka");
    DefaultKafkaConsumerFactory<String, StockProcessedEvent> cf =
            new DefaultKafkaConsumerFactory<>(
                    props, new StringDeserializer()
                    , jsonDeserializer
            );
    testConsumer = cf.createConsumer();
    testConsumer.subscribe(List.of(KafkaConfiguration.STOCK_PROCESSED_TOPIC));
  }

  @Test
  void shouldCancelItemsReservationAfterFailedPayment() {
    int productId = 1;
    int initialQuantity = 10;
    productRepository.save(new Product(productId, initialQuantity));
    List<OrderItemDto> orderItems = List.of(new OrderItemDto(1, null, 5));
    OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(
            1, "1", 1, null, orderItems);
    kafkaTemplate.send(
            KafkaConfiguration.ORDER_CREATED_TOPIC, "1", orderCreatedEvent);


    await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() ->
            {

              Product p = productRepository.findById(productId)
                      .orElseThrow();
              Assertions.assertEquals(5, p.getQuantity());
            });


    ConsumerRecord<String, StockProcessedEvent> record =
            KafkaTestUtils.getSingleRecord(
                    testConsumer,
                    KafkaConfiguration.STOCK_PROCESSED_TOPIC
            );

    assertThat(record.value()
            .stockAvailable()).isTrue();
    assertThat(record.value()
            .orderId()).isEqualTo(1);

    PaymentProcessedEvent paymentProcessedEvent = new PaymentProcessedEvent(
            1
            , "1", false, "Payment failed", orderItems
    );
    kafkaTemplate.send(
            KafkaConfiguration.PAYMENT_PROCESS_TOPIC,
            "1", paymentProcessedEvent
    );
    await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
              Product p = productRepository.findById(productId)
                      .orElseThrow();
              Assertions.assertEquals(initialQuantity, p.getQuantity());
            });

  }

  @Test
  void shouldFollowSuccessfullPath() {
    List<OrderItemDto> orderItems = List.of(new OrderItemDto(1, null, 1));
    OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(
            1, "1", 1, null,
            orderItems
    );
    StockProcessedEvent stockProcessedEvent = new StockProcessedEvent(
            1,
            1, "1", true, null, "", orderItems
    );
    PaymentProcessedEvent paymentProcessedEvent = new PaymentProcessedEvent(
            1,
            "1",
            true,
            null,
            orderItems
    );

    productRepository.save(new Product(1, 10));
    kafkaTemplate.send(
            KafkaConfiguration.ORDER_CREATED_TOPIC, "1", orderCreatedEvent
    );
    await().atMost(Duration.ofSeconds(5)).untilAsserted(  () -> {
        Product p = productRepository.findById(1)
                .orElseThrow();
        Assertions.assertEquals(9, p.getQuantity());
    } );
    ConsumerRecord<String, StockProcessedEvent> stockRecord =
            KafkaTestUtils.getSingleRecord(
                    testConsumer,
                    KafkaConfiguration.STOCK_PROCESSED_TOPIC
            );
    Assertions.assertTrue( stockRecord.value().stockAvailable());

    kafkaTemplate.send(
            KafkaConfiguration.PAYMENT_PROCESS_TOPIC, "1",
            paymentProcessedEvent
    );


    await().atMost(Duration.ofSeconds(5)).untilAsserted(  () -> {
        Product p = productRepository.findById(1)
                .orElseThrow();
        Assertions.assertEquals(9, p.getQuantity());
    } );
  }
}
