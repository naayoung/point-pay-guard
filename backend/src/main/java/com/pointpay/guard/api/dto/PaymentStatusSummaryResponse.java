package com.pointpay.guard.api.dto;

import com.pointpay.guard.domain.payment.PaymentStatus;

/**
 * 결제 상태 하나에 해당하는 전체 결제 건수와 금액 합계를 반환한다.
 */
public record PaymentStatusSummaryResponse(
        PaymentStatus status,
        long paymentCount,
        long totalAmount
) {
}
