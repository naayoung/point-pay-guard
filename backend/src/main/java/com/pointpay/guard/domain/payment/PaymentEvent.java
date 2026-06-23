package com.pointpay.guard.domain.payment;

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
import java.time.Instant;

@Entity
@Table(name = "payment_events")
public class PaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PaymentEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PaymentStatus beforeStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus afterStatus;

    @Column(nullable = false, length = 300)
    private String reason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected PaymentEvent() {
    }

    private PaymentEvent(
            Payment payment,
            PaymentEventType eventType,
            PaymentStatus beforeStatus,
            PaymentStatus afterStatus,
            String reason
    ) {
        this.payment = payment;
        this.eventType = eventType;
        this.beforeStatus = beforeStatus;
        this.afterStatus = afterStatus;
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    public static PaymentEvent of(
            Payment payment,
            PaymentEventType eventType,
            PaymentStatus beforeStatus,
            PaymentStatus afterStatus,
            String reason
    ) {
        return new PaymentEvent(payment, eventType, beforeStatus, afterStatus, reason);
    }

    public Long getId() {
        return id;
    }

    public Payment getPayment() {
        return payment;
    }

    public PaymentEventType getEventType() {
        return eventType;
    }

    public PaymentStatus getBeforeStatus() {
        return beforeStatus;
    }

    public PaymentStatus getAfterStatus() {
        return afterStatus;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
