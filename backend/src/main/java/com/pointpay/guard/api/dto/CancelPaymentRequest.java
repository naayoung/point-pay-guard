package com.pointpay.guard.api.dto;

public record CancelPaymentRequest(String reason) {

    public String normalizedReason() {
        if (reason == null || reason.isBlank()) {
            return "사용자 결제 취소 요청";
        }
        return reason.trim();
    }
}
