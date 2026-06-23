package com.pointpay.guard.domain.payment;

import java.util.Set;

public enum PaymentStatus {
    READY,
    APPROVING,
    APPROVED,
    FAILED,
    CANCELING,
    CANCELED,
    SETTLED;

    public boolean canTransitionTo(PaymentStatus nextStatus) {
        // 결제 상태는 정해진 흐름으로만 이동하게 해서 정산 후 취소 같은 역전이를 막는다.
        return switch (this) {
            case READY -> Set.of(APPROVING).contains(nextStatus);
            case APPROVING -> Set.of(APPROVED, FAILED).contains(nextStatus);
            case APPROVED -> Set.of(CANCELING, SETTLED).contains(nextStatus);
            case CANCELING -> Set.of(CANCELED).contains(nextStatus);
            case FAILED, CANCELED, SETTLED -> false;
        };
    }
}
