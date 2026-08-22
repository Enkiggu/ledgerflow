package com.ledgerflow.order.dto;

import com.ledgerflow.order.domain.OrderItem;

import java.time.Instant;

public record OrderItemResponse(
        String id,
        String productId,
        int quantity,
        long unitPriceCents,
        long totalPriceCents,
        Instant createdAt
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getQuantity(),
                item.getUnitPriceCents(),
                item.getTotalPriceCents(),
                item.getCreatedAt()
        );
    }
}
