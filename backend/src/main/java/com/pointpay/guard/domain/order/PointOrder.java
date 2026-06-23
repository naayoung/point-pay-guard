package com.pointpay.guard.domain.order;

import com.pointpay.guard.domain.user.PointUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class PointOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private PointUser user;

    @Column(name = "order_amount", nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private Long version;

    protected PointOrder() {
    }

    public PointOrder(PointUser user, Long amount) {
        this.user = user;
        this.amount = amount;
        this.status = OrderStatus.CREATED;
        this.createdAt = Instant.now();
    }

    public boolean canStartPayment() {
        return status == OrderStatus.CREATED || status == OrderStatus.PAYMENT_FAILED;
    }

    public void markPaying() {
        this.status = OrderStatus.PAYING;
    }

    public void markPaid() {
        this.status = OrderStatus.PAID;
    }

    public void markPaymentFailed() {
        this.status = OrderStatus.PAYMENT_FAILED;
    }

    public void markCanceled() {
        this.status = OrderStatus.CANCELED;
    }

    public void markSettled() {
        this.status = OrderStatus.SETTLED;
    }

    public Long getId() {
        return id;
    }

    public PointUser getUser() {
        return user;
    }

    public Long getAmount() {
        return amount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
