package com.pointpay.guard.domain.payment;

import com.pointpay.guard.domain.order.PointOrder;
import com.pointpay.guard.domain.user.PointUser;
import com.pointpay.guard.global.exception.ErrorCode;
import com.pointpay.guard.global.exception.InvalidPaymentStateException;
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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payments_idempotency_key", columnNames = "idempotency_key")
        }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private PointOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private PointUser user;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    private Instant approvedAt;

    private Instant canceledAt;

    private Instant settledAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private Long version;

    protected Payment() {
    }

    public Payment(PointOrder order, String idempotencyKey) {
        this.order = order;
        this.user = order.getUser();
        this.amount = order.getAmount();
        this.status = PaymentStatus.READY;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Instant.now();
    }

    public PaymentStatus transitionTo(PaymentStatus nextStatus) {
        // 상태 변경은 이 메서드를 통과하게 만들어 도메인 규칙을 한 곳에서 검증한다.
        if (!status.canTransitionTo(nextStatus)) {
            throw new InvalidPaymentStateException(
                    ErrorCode.INVALID_PAYMENT_STATE,
                    "결제 상태를 " + status + "에서 " + nextStatus + "(으)로 변경할 수 없습니다."
            );
        }

        PaymentStatus before = this.status;
        this.status = nextStatus;
        Instant now = Instant.now();
        if (nextStatus == PaymentStatus.APPROVED) {
            this.approvedAt = now;
        }
        if (nextStatus == PaymentStatus.CANCELED) {
            this.canceledAt = now;
        }
        if (nextStatus == PaymentStatus.SETTLED) {
            this.settledAt = now;
        }
        return before;
    }

    public Long getId() {
        return id;
    }

    public PointOrder getOrder() {
        return order;
    }

    public PointUser getUser() {
        return user;
    }

    public Long getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public Instant getCanceledAt() {
        return canceledAt;
    }

    public Instant getSettledAt() {
        return settledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
