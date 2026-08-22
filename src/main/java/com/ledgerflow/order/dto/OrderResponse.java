package com.ledgerflow.order.dto;

import com.ledgerflow.order.domain.Order;
import com.ledgerflow.order.domain.OrderStatus;

import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String id,
        String customerId,
        OrderStatus status,
        String currency,
        long totalAmountCents,
        Long version,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getCurrency(),
                order.getTotalAmountCents(),
                order.getVersion(),
                order.getItems() != null
                        ? order.getItems().stream().map(OrderItemResponse::from).toList()
                        : List.of(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
