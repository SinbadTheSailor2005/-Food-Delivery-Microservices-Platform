package dev.vundirov.orderservice;


import dev.vundirov.app.KafkaConfiguration;
import dev.vundirov.common.dto.PaymentStatus;
import dev.vundirov.common.dto.kafka.OrderCreatedEvent;
import dev.vundirov.common.dto.kafka.PaymentProcessedEvent;
import dev.vundirov.common.dto.kafka.StockProcessedEvent;
import dev.vundirov.orderservice.domain.api.OrderService;
import dev.vundirov.orderservice.domain.api.dto.PostOrderDto;
import dev.vundirov.orderservice.domain.dto.OrderMapper;
import dev.vundirov.orderservice.domain.entities.Order;
import dev.vundirov.orderservice.domain.entities.repositories.OrderRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEntityServiceApplicationTest {

  @Spy
  private OrderMapper orderMapper = Mappers.getMapper(OrderMapper.class);
  @Mock
  private OrderRepository orderRepository;
  @Mock
  private KafkaTemplate<String, Object> kafkaTemplate;

  @InjectMocks
  private OrderService orderService;

  @Test
  void givenWareHouseEvent_whenStockNotAvailable_thenShouldSetFailedStatus() {
    int orderId = 123;
    StockProcessedEvent event = new StockProcessedEvent(
            orderId,
            1,
            "msg-001",
            false,
            null,
            "Stock not available",
            null
    );

    Order order = new Order();
    order.setId(orderId);
    order.setPaymentStatus(PaymentStatus.PENDING);

    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
    orderService.examineWareHouseEvent(event);

    Assertions.assertEquals(PaymentStatus.FAILED, order.getPaymentStatus());
    verify(orderRepository, times(1)).save(order);


  }

  @Test
  void givenPaymentEvent_whenPaymentFailed_shouldSetFailedPaymentStatus() {
    int orderId = 123;
    PaymentProcessedEvent event =
            new PaymentProcessedEvent(orderId, "msg-001", false, null, null);
    Order order = new Order();
    order.setId(orderId);
    order.setPaymentStatus(PaymentStatus.PENDING);

    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
    orderService.examinePaymentEvent(event);

    Assertions.assertEquals(PaymentStatus.FAILED, order.getPaymentStatus());
    verify(orderRepository, times(1)).save(order);
  }


  @Test
  void givenPaymentEvent_whenPaymentSuccess_shouldSetCompletedPaymentStatus() {
    int orderId = 123;
    PaymentProcessedEvent event =
            new PaymentProcessedEvent(orderId, "msg-001", true, null, null);
    Order order = new Order();
    order.setId(orderId);
    order.setPaymentStatus(PaymentStatus.PENDING);

    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
    orderService.examinePaymentEvent(event);

    Assertions.assertEquals(PaymentStatus.COMPLETED, order.getPaymentStatus());
    verify(orderRepository, times(1)).save(order);
  }

  @Test
  void givenPostOrderDto_whenCreate_shouldSaveOrderAndSendEventToKafka() {
    PostOrderDto postOrderDto = new PostOrderDto(
            1, BigDecimal.valueOf(100),
            Set.of()
    );



    Order postSave = new Order();
    postSave.setPaymentStatus(PaymentStatus.PENDING);
    postSave.setOrderItems(Set.of());
    postSave.setUserId(1);
    postSave.setId(1);
    postSave.setTotalCost(BigDecimal.valueOf(100));

    OrderCreatedEvent orderCreatedEvent =
            orderMapper.toOrderCreatedEvent(postSave);
    when(orderRepository.save(any(Order.class))).thenReturn(postSave);
    when(orderMapper.toOrderCreatedEvent(any(Order.class))).thenReturn(orderCreatedEvent);
    orderService.create(postOrderDto);

    verify(orderMapper, times(1)).toEntity(postOrderDto);
    verify(orderRepository, times(1)).save(argThat( (order -> order.getId() == null &&
            order.getOrderItems().isEmpty() && order.getUserId() == 1 && order.getPaymentStatus() == PaymentStatus.PENDING && Objects.equals(
            order.getTotalCost(), BigDecimal.valueOf(100))

            ) ));
    verify(kafkaTemplate, times(1)).send(eq(KafkaConfiguration.ORDER_CREATED_TOPIC),
            eq("1"),
            eq(orderCreatedEvent)
            );
  }
}
