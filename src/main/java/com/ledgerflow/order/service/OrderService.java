package com.ledgerflow.order.service;

import com.ledgerflow.cache.domain.Customer;
import com.ledgerflow.cache.domain.Product;
import com.ledgerflow.cache.service.CustomerCacheService;
import com.ledgerflow.cache.service.ProductCacheService;
import com.ledgerflow.common.exception.DomainException;
import com.ledgerflow.common.exception.ErrorCode;
import com.ledgerflow.common.model.Money;
import com.ledgerflow.messaging.event.OrderCreatedEvent;
import com.ledgerflow.order.domain.Order;
import com.ledgerflow.order.domain.OrderItem;
import com.ledgerflow.order.domain.OrderStatus;
import com.ledgerflow.order.dto.CreateOrderRequest;
import com.ledgerflow.order.dto.OrderItemRequest;
import com.ledgerflow.order.dto.OrderResponse;
import com.ledgerflow.order.repository.OrderRepository;
import com.ledgerflow.outbox.service.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CustomerCacheService customerCacheService;
    private final ProductCacheService productCacheService;
    private final OutboxService outboxService;

    public OrderService(OrderRepository orderRepository,
                        CustomerCacheService customerCacheService,
                        ProductCacheService productCacheService,
                        OutboxService outboxService) {
        this.orderRepository = orderRepository;
        this.customerCacheService = customerCacheService;
        this.productCacheService = productCacheService;
        this.outboxService = outboxService;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer: {} with {} items", request.customerId(), request.items().size());

        Customer customer = customerCacheService.getCustomerById(request.customerId());
        if (!"ACTIVE".equalsIgnoreCase(customer.getStatus())) {
            throw new DomainException(ErrorCode.INVALID_ARGUMENT, "Customer account is inactive");
        }

        String orderId = UUID.randomUUID().toString();
        long calculatedTotalCents = 0;

        Order order = Order.builder()
                .id(orderId)
                .customerId(customer.getId())
                .status(OrderStatus.CREATED)
                .currency(request.currency().toUpperCase())
                .version(0L)
                .build();

        for (OrderItemRequest itemReq : request.items()) {
            Product product = productCacheService.getProductById(itemReq.productId());

            if (!product.getCurrency().equalsIgnoreCase(request.currency())) {
                throw new DomainException(ErrorCode.CURRENCY_MISMATCH,
                        String.format("Product %s currency %s does not match order currency %s",
                                product.getSku(), product.getCurrency(), request.currency()));
            }

            long itemTotalCents = Math.multiplyExact(itemReq.unitPrice(), (long) itemReq.quantity());
            calculatedTotalCents = Math.addExact(calculatedTotalCents, itemTotalCents);

            OrderItem item = OrderItem.builder()
                    .id(UUID.randomUUID().toString())
                    .order(order)
                    .productId(product.getId())
                    .quantity(itemReq.quantity())
                    .unitPriceCents(itemReq.unitPrice())
                    .totalPriceCents(itemTotalCents)
                    .build();

            order.addItem(item);
        }

        order.setTotalAmountCents(calculatedTotalCents);
        Order savedOrder = orderRepository.save(order);

        // Transactional outbox event
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                savedOrder.getId(),
                savedOrder.getCustomerId(),
                savedOrder.getTotalAmountCents(),
                savedOrder.getCurrency(),
                Instant.now()
        );
        outboxService.saveEvent("ORDER", savedOrder.getId(), event);

        log.info("Order created successfully [id: {}, total: {} {}]", savedOrder.getId(), savedOrder.getTotalAmountCents(), savedOrder.getCurrency());
        return OrderResponse.from(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new DomainException(ErrorCode.ORDER_NOT_FOUND, "Order not found with id: " + orderId));
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(String customerId, OrderStatus status, Pageable pageable) {
        Page<Order> page;
        if (customerId != null && status != null) {
            page = orderRepository.findByCustomerIdAndStatus(customerId, status, pageable);
        } else if (customerId != null) {
            page = orderRepository.findByCustomerId(customerId, pageable);
        } else if (status != null) {
            page = orderRepository.findByStatus(status, pageable);
        } else {
            page = orderRepository.findAll(pageable);
        }
        return page.map(OrderResponse::from);
    }

    @Transactional
    public OrderResponse cancelOrder(String orderId) {
        log.info("Attempting cancellation for order: {}", orderId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new DomainException(ErrorCode.ORDER_NOT_FOUND, "Order not found: " + orderId));

        order.transitionTo(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        log.info("Order cancelled successfully: {}", orderId);
        return OrderResponse.from(saved);
    }
}
