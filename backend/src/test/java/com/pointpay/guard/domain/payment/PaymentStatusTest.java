package com.pointpay.guard.domain.payment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PaymentStatusTest {

    @Test
    void allowsOnlyDefinedPaymentStateTransitions() {
        assertThat(PaymentStatus.READY.canTransitionTo(PaymentStatus.APPROVING)).isTrue();
        assertThat(PaymentStatus.APPROVING.canTransitionTo(PaymentStatus.APPROVED)).isTrue();
        assertThat(PaymentStatus.APPROVING.canTransitionTo(PaymentStatus.FAILED)).isTrue();
        assertThat(PaymentStatus.APPROVED.canTransitionTo(PaymentStatus.CANCELING)).isTrue();
        assertThat(PaymentStatus.APPROVED.canTransitionTo(PaymentStatus.SETTLED)).isTrue();
        assertThat(PaymentStatus.CANCELING.canTransitionTo(PaymentStatus.CANCELED)).isTrue();

        assertThat(PaymentStatus.FAILED.canTransitionTo(PaymentStatus.APPROVED)).isFalse();
        assertThat(PaymentStatus.CANCELED.canTransitionTo(PaymentStatus.APPROVED)).isFalse();
        assertThat(PaymentStatus.SETTLED.canTransitionTo(PaymentStatus.CANCELING)).isFalse();
    }
}
