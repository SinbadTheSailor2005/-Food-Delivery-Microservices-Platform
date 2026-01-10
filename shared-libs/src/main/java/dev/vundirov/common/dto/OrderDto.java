package dev.vundirov.common.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Set;


public record OrderDto(Integer id, @NotNull PaymentStatus paymentStatus,
                       @NotNull Integer userId, @NotNull BigDecimal totalCost,
                       Set<OrderItemDto> orderItems) {
}