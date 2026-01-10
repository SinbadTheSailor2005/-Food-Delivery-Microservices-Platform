package dev.vundirov.app;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

@Configuration
public class KafkaConfiguration {

  public static final String PAYMENT_PROCESS_TOPIC = "payment-process";
  public static final String ORDER_CREATED_TOPIC = "order-created";
  public static final String STOCK_PROCESSED_TOPIC = "stock-processed";
  private final String DEFAULT_BOOTSTRAP_SERVER = "localhost:9092";
  @Bean
  DefaultKafkaProducerFactory<String, Object> objectProducerFactory(
          KafkaProperties properties) {
    Map<String, Object> producerProperties =
            properties.buildProducerProperties(null);
    producerProperties.putIfAbsent(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, DEFAULT_BOOTSTRAP_SERVER);
    producerProperties.put(
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    producerProperties.put(
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    producerProperties.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);
    return new DefaultKafkaProducerFactory<>(producerProperties);
  }

  @Bean
  KafkaTemplate<String, Object> objectKafkaTemplate(
          DefaultKafkaProducerFactory<String, Object> objectProducerFactory) {
    return new KafkaTemplate<>(objectProducerFactory);
  }

  @Bean
  public ConsumerFactory<String, Object> objectConsumerFactory(
          KafkaProperties kafkaProperties) {
    Map<String, Object> props = kafkaProperties.buildConsumerProperties(null);
    props.putIfAbsent(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
            DEFAULT_BOOTSTRAP_SERVER);
    props.put(
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            StringDeserializer.class
    );
    props.put(
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            JsonDeserializer.class
    );
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "java.lang,dev.vundirov" +
            ".common.dto.kafka");
    return new DefaultKafkaConsumerFactory<>(props);
  }

  @Bean
  public KafkaListenerContainerFactory<?> objectListenerFactory(
          ConsumerFactory<String, Object> objectConsumerFactory) {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(objectConsumerFactory);
    factory.setBatchListener(false);
    return factory;
  }

  @Bean
  NewTopic paymentProcessTopic() {
    return TopicBuilder.name(PAYMENT_PROCESS_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
  }

  @Bean
  NewTopic orderCreatedTopic() {
    return TopicBuilder.name(ORDER_CREATED_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
  }

  @Bean
  NewTopic stockProcessedTopic() {
    return TopicBuilder.name(STOCK_PROCESSED_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
  }

}