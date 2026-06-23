package com.pointpay.guard.service;

import com.pointpay.guard.api.dto.ApprovePaymentRequest;
import com.pointpay.guard.api.dto.CancelPaymentRequest;
import com.pointpay.guard.api.dto.PaymentResponse;
import com.pointpay.guard.api.dto.SettlementResponse;
import com.pointpay.guard.domain.order.PointOrder;
import com.pointpay.guard.domain.payment.Payment;
import com.pointpay.guard.domain.payment.PaymentEvent;
import com.pointpay.guard.domain.payment.PaymentEventType;
import com.pointpay.guard.domain.payment.PaymentStatus;
import com.pointpay.guard.domain.wallet.Wallet;
import com.pointpay.guard.global.exception.BusinessException;
import com.pointpay.guard.global.exception.ErrorCode;
import com.pointpay.guard.global.exception.InsufficientBalanceException;
import com.pointpay.guard.global.exception.ResourceNotFoundException;
import com.pointpay.guard.repository.OrderRepository;
import com.pointpay.guard.repository.PaymentEventRepository;
import com.pointpay.guard.repository.PaymentRepository;
import com.pointpay.guard.repository.WalletRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentTransactionService {

    private static final List<PaymentStatus> ACTIVE_PAYMENT_STATUSES = List.of(
            PaymentStatus.APPROVING,
            PaymentStatus.APPROVED,
            PaymentStatus.CANCELING,
            PaymentStatus.SETTLED
    );

    private final OrderRepository orderRepository;
    private final WalletRepository walletRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;

    public PaymentTransactionService(
            OrderRepository orderRepository,
            WalletRepository walletRepository,
            PaymentRepository paymentRepository,
            PaymentEventRepository paymentEventRepository
    ) {
        this.orderRepository = orderRepository;
        this.walletRepository = walletRepository;
        this.paymentRepository = paymentRepository;
        this.paymentEventRepository = paymentEventRepository;
    }

    @Transactional
    public PaymentResponse approve(ApprovePaymentRequest request) {
        // Redis TTL 만료나 DB 재조회 상황에서도 unique key 기준으로 한 번 더 멱등성을 보장한다.
        return paymentRepository.findDetailByIdempotencyKey(request.idempotencyKey())
                .map(PaymentResponse::from)
                .orElseGet(() -> approveNewPayment(request));
    }

    @Transactional
    public PaymentResponse cancel(Long paymentId, CancelPaymentRequest request) {
        Payment payment = paymentRepository.findWithLockById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.PAYMENT_NOT_FOUND,
                        "결제 정보를 찾을 수 없습니다. paymentId=" + paymentId
                ));
        Wallet wallet = walletRepository.findWithLockByUserId(payment.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.WALLET_NOT_FOUND,
                        "사용자 지갑을 찾을 수 없습니다. userId=" + payment.getUser().getId()
                ));

        transition(payment, PaymentStatus.CANCELING, request.normalizedReason());
        wallet.deposit(payment.getAmount());
        payment.getOrder().markCanceled();
        transition(payment, PaymentStatus.CANCELED, "포인트 환불 완료");

        return PaymentResponse.from(payment);
    }

    @Transactional
    public SettlementResponse settleApprovedPayments() {
        List<Payment> payments = paymentRepository.findWithLockByStatus(PaymentStatus.APPROVED);
        Long totalAmount = 0L;

        for (Payment payment : payments) {
            transition(payment, PaymentStatus.SETTLED, "정산 배치 처리");
            payment.getOrder().markSettled();
            totalAmount += payment.getAmount();
        }

        List<Long> settledPaymentIds = payments.stream()
                .map(Payment::getId)
                .toList();
        return new SettlementResponse(settledPaymentIds.size(), totalAmount, settledPaymentIds);
    }

    private PaymentResponse approveNewPayment(ApprovePaymentRequest request) {
        // 주문과 지갑은 쓰기 락으로 조회해 승인/취소/정산 중 상태와 잔액이 엇갈리지 않게 한다.
        PointOrder order = orderRepository.findWithLockById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ORDER_NOT_FOUND,
                        "주문 정보를 찾을 수 없습니다. orderId=" + request.orderId()
                ));

        if (!order.canStartPayment()) {
            throw new BusinessException(
                    ErrorCode.INVALID_ORDER_STATE,
                    "결제를 시작할 수 없는 주문 상태입니다. orderId=" + order.getId() + ", status=" + order.getStatus()
            );
        }

        paymentRepository.findFirstByOrderIdAndStatusIn(order.getId(), ACTIVE_PAYMENT_STATUSES)
                .ifPresent(payment -> {
                    throw new BusinessException(
                            ErrorCode.INVALID_ORDER_STATE,
                            "이미 처리 중이거나 완료된 결제가 있습니다. orderId="
                                    + order.getId() + ", paymentId=" + payment.getId()
                    );
                });

        Wallet wallet = walletRepository.findWithLockByUserId(order.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.WALLET_NOT_FOUND,
                        "사용자 지갑을 찾을 수 없습니다. userId=" + order.getUser().getId()
                ));

        Payment payment = paymentRepository.save(new Payment(order, request.idempotencyKey()));
        record(payment, null, PaymentStatus.READY, "결제 요청 생성");
        transition(payment, PaymentStatus.APPROVING, "결제 승인 처리 시작");
        order.markPaying();

        try {
            wallet.withdraw(order.getAmount());
        } catch (InsufficientBalanceException e) {
            // 잔액 부족은 결제 실패 상태로 남겨 원인과 상태 변경 이력을 조회할 수 있게 한다.
            transition(payment, PaymentStatus.FAILED, e.getMessage());
            order.markPaymentFailed();
            return PaymentResponse.from(payment);
        }

        transition(payment, PaymentStatus.APPROVED, "포인트 차감 및 결제 승인 완료");
        order.markPaid();
        return PaymentResponse.from(payment);
    }

    private void transition(Payment payment, PaymentStatus nextStatus, String reason) {
        PaymentStatus beforeStatus = payment.transitionTo(nextStatus);
        record(payment, beforeStatus, nextStatus, reason);
    }

    private void record(Payment payment, PaymentStatus beforeStatus, PaymentStatus afterStatus, String reason) {
        // 금융성 흐름은 현재 상태만큼 변경 이력이 중요하므로 모든 상태 전이를 이벤트로 남긴다.
        paymentEventRepository.save(PaymentEvent.of(
                payment,
                PaymentEventType.fromAfterStatus(afterStatus),
                beforeStatus,
                afterStatus,
                reason
        ));
    }
}
