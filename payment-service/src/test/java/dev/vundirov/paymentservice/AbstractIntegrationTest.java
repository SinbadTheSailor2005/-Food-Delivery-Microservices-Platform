package dev.vundirov.paymentservice;


import org.mockito.Spy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest  {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
          "postgres:15-alpine");

  @Container
  static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse(
          "apache/kafka"));

  @DynamicPropertySource
  static void dynamicProperties (DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);

    registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    registry.add("spring.jpa.show-sql", () -> "true");
    registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
  }
}
