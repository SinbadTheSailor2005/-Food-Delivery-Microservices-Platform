package dev.vundirov.orderservice.domain.api;

import dev.vundirov.app.KafkaConfiguration;
import dev.vundirov.common.dto.kafka.OrderCreatedEvent;
import dev.vundirov.orderservice.domain.api.dto.PostOrderDto;
import dev.vundirov.common.dto.OrderDto;
import dev.vundirov.orderservice.domain.dto.OrderMapper;
import dev.vundirov.orderservice.domain.entities.Order;
import dev.vundirov.orderservice.domain.entities.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class OrderService {

  private final OrderMapper orderMapper;

  private final OrderRepository orderRepository;
private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
  private final KafkaTemplate<String, Object> kafkaTemplate;

  public PostOrderDto create(PostOrderDto dto) {
    logger.info("Creating a new order with data: {}", dto);
    Order order = orderMapper.toEntity(dto);
    Order resultOrder = orderRepository.save(order);
    OrderCreatedEvent orderCreatedEvent =
            orderMapper.toOrderCreatedEvent(order);
    logger.info("Sending to kafka {} object {}", KafkaConfiguration.ORDER_CREATED_TOPIC,
            orderCreatedEvent
    );
    kafkaTemplate.send(KafkaConfiguration.ORDER_CREATED_TOPIC,
            orderCreatedEvent.orderId().toString() ,
            orderCreatedEvent
    );
    return orderMapper.toPostOrderDto(resultOrder);
  }

  public OrderDto getOne(Integer id) {
    logger.info("Fetching order with id: {}", id);
    Optional<Order> orderOptional = orderRepository.findById(id);
    return orderMapper.toOrderDto(orderOptional.orElseThrow(() ->
            new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Entity with id `%s` not found".formatted(id)
            )));
  }
}
