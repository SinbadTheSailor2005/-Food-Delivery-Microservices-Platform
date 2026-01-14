package dev.vundirov.paymentservice;

import dev.vundirov.app.KafkaConfiguration;
import dev.vundirov.common.dto.kafka.StockProcessedEvent;
import dev.vundirov.paymentservice.domain.enities.User;
import dev.vundirov.paymentservice.domain.repositories.IdempotencyKeyRepository;
import dev.vundirov.paymentservice.domain.repositories.UserRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import static org.awaitility.Awaitility.await;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class PaymentServiceIT extends AbstractIntegrationTest{

  @Autowired
  private UserRepository userRepository;
   @Autowired
  private IdempotencyKeyRepository idempotencyKeyRepository;
  @Autowired
  private KafkaTemplate<String, Object> kafkaTemplate;

  private Consumer<String, StockProcessedEvent> testConsumer;


  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
    idempotencyKeyRepository.deleteAll();

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
  void testDublicateEvents() throws InterruptedException {
    User user = new User(null, BigDecimal.valueOf(100));
    User savedUser = userRepository.save(user);
    StockProcessedEvent stockProcessedEvent = new StockProcessedEvent(
            1,
            savedUser.getId(),
            "msg-1",
            true,
            BigDecimal.valueOf(50),
            "Stock reserved",
            List.of()
    );

    kafkaTemplate.send(KafkaConfiguration.STOCK_PROCESSED_TOPIC,"1", stockProcessedEvent);

    kafkaTemplate.send(KafkaConfiguration.STOCK_PROCESSED_TOPIC,"1", stockProcessedEvent);


    ConsumerRecords<String, StockProcessedEvent> records =
            KafkaTestUtils.getRecords(
                    testConsumer
            );
    Assertions.assertEquals(2, records.count());
    await().atMost(Duration.ofSeconds(5)).untilAsserted( () -> {
      Assertions.assertTrue(idempotencyKeyRepository.existsById("msg-1"));
    } );

    Thread.sleep(1000);

    Assertions.assertEquals(50, userRepository.findById(1).orElseThrow().getBalance().intValue());
  }

}
