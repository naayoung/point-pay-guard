package com.pointpay.guard.api.dto;

import java.util.List;

public record SettlementResponse(
        int settledCount,
        Long totalSettledAmount,
        List<Long> settledPaymentIds
) {
}
