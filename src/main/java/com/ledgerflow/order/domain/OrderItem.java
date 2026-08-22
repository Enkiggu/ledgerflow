package com.ledgerflow.order.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ledgerflow.common.model.Money;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem implements Serializable {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price_cents", nullable = false)
    private long unitPriceCents;

    @Column(name = "total_price_cents", nullable = false)
    private long totalPriceCents;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (totalPriceCents == 0 && unitPriceCents > 0) {
            totalPriceCents = unitPriceCents * quantity;
        }
    }

    public Money getUnitPrice(String currency) {
        return Money.of(this.unitPriceCents, currency);
    }

    public Money getTotalPrice(String currency) {
        return Money.of(this.totalPriceCents, currency);
    }
}
