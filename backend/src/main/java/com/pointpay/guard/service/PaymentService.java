package com.pointpay.guard.service;

import com.pointpay.guard.api.dto.ApprovePaymentRequest;
import com.pointpay.guard.api.dto.CancelPaymentRequest;
import com.pointpay.guard.api.dto.PaymentResponse;
import com.pointpay.guard.api.dto.SettlementResponse;
import com.pointpay.guard.global.exception.DuplicatePaymentRequestException;
import com.pointpay.guard.global.exception.ErrorCode;
import com.pointpay.guard.global.exception.ResourceNotFoundException;
import com.pointpay.guard.service.redis.IdempotencyGuard;
import com.pointpay.guard.service.redis.PaymentLockManager;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final PaymentTransactionService paymentTransactionService;
    private final PaymentQueryService paymentQueryService;
    private final IdempotencyGuard idempotencyGuard;
    private final PaymentLockManager paymentLockManager;

    public PaymentService(
            PaymentTransactionService paymentTransactionService,
            PaymentQueryService paymentQueryService,
            IdempotencyGuard idempotencyGuard,
            PaymentLockManager paymentLockManager
    ) {
        this.paymentTransactionService = paymentTransactionService;
        this.paymentQueryService = paymentQueryService;
        this.idempotencyGuard = idempotencyGuard;
        this.paymentLockManager = paymentLockManager;
    }

    public PaymentResponse approve(ApprovePaymentRequest request) {
        // Redis 멱등성 키를 먼저 선점해서 같은 결제 요청이 동시에 들어오는 것을 입구에서 막는다.
        boolean claimed = idempotencyGuard.claim(request.idempotencyKey());
        if (!claimed) {
            return existingResultOrInProgress(request.idempotencyKey());
        }

        try {
            // 같은 주문에 대한 승인 처리는 하나만 통과시키고, 실제 정합성은 DB 트랜잭션에서 보장한다.
            PaymentResponse response = paymentLockManager.execute(
                    "order:" + request.orderId(),
                    () -> paymentTransactionService.approve(request)
            );
            idempotencyGuard.complete(request.idempotencyKey(), response.paymentId());
            return response;
        } catch (RuntimeException e) {
            // 처리 실패 시 같은 멱등성 키로 재시도할 수 있도록 Redis 선점 키를 해제한다.
            idempotencyGuard.release(request.idempotencyKey());
            throw e;
        }
    }

    public PaymentResponse cancel(Long paymentId, CancelPaymentRequest request) {
        return paymentLockManager.execute(
                "payment:" + paymentId,
                () -> paymentTransactionService.cancel(paymentId, request)
        );
    }

    public SettlementResponse settleApprovedPayments() {
        return paymentLockManager.execute(
                "settlement:run",
                paymentTransactionService::settleApprovedPayments
        );
    }

    private PaymentResponse existingResultOrInProgress(String idempotencyKey) {
        try {
            // 이미 완료된 동일 요청이면 새로 결제하지 않고 기존 결제 결과를 돌려준다.
            return paymentQueryService.getPaymentByIdempotencyKey(idempotencyKey);
        } catch (ResourceNotFoundException e) {
            throw new DuplicatePaymentRequestException(
                    ErrorCode.DUPLICATE_PAYMENT_REQUEST,
                    "동일한 멱등성 키의 결제 요청이 처리 중입니다. idempotencyKey=" + idempotencyKey
            );
        }
    }
}
