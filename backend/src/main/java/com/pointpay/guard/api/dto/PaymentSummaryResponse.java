package com.pointpay.guard.api.dto;

import java.util.List;

/**
 * 전체 결제 현황과 정산 완료 현황을 한 번에 조회할 수 있는 집계 응답이다.
 * totalPaymentAmount는 결제 성공 여부와 관계없이 생성된 모든 결제 요청 금액의 합계다.
 */
public record PaymentSummaryResponse(
        long totalPaymentCount,
        long totalPaymentAmount,
        long settledPaymentCount,
        long settledPaymentAmount,
        List<PaymentStatusSummaryResponse> statusSummaries
) {
}
