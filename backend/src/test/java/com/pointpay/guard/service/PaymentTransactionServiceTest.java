package com.pointpay.guard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pointpay.guard.api.dto.ApprovePaymentRequest;
import com.pointpay.guard.api.dto.CancelPaymentRequest;
import com.pointpay.guard.api.dto.CreateOrderRequest;
import com.pointpay.guard.api.dto.OrderResponse;
import com.pointpay.guard.api.dto.PaymentResponse;
import com.pointpay.guard.domain.order.OrderStatus;
import com.pointpay.guard.domain.payment.PaymentStatus;
import com.pointpay.guard.global.exception.InvalidPaymentStateException;
import com.pointpay.guard.repository.OrderRepository;
import com.pointpay.guard.repository.PaymentEventRepository;
import com.pointpay.guard.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PaymentTransactionServiceTest {

    private static final Long DEMO_USER_ID = 1L;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentTransactionService paymentTransactionService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentEventRepository paymentEventRepository;

    @Test
    void approveWithdrawsWalletAndRecordsPaymentEvents() {
        long beforeBalance = walletBalance();
        OrderResponse order = orderService.create(new CreateOrderRequest(DEMO_USER_ID, 10_000L));

        PaymentResponse payment = paymentTransactionService.approve(
                new ApprovePaymentRequest(order.orderId(), "approve-key-1")
        );

        assertThat(payment.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(walletBalance()).isEqualTo(beforeBalance - 10_000L);
        assertThat(paymentEventRepository.findByPaymentIdOrderByIdAsc(payment.paymentId()))
                .extracting(event -> event.getAfterStatus())
                .containsExactly(PaymentStatus.READY, PaymentStatus.APPROVING, PaymentStatus.APPROVED);
    }

    @Test
    void approveWithInsufficientBalanceMarksPaymentFailedAndKeepsWalletBalance() {
        long beforeBalance = walletBalance();
        OrderResponse order = orderService.create(new CreateOrderRequest(DEMO_USER_ID, beforeBalance + 1L));

        PaymentResponse payment = paymentTransactionService.approve(
                new ApprovePaymentRequest(order.orderId(), "approve-key-2")
        );

        assertThat(payment.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(walletBalance()).isEqualTo(beforeBalance);
        assertThat(orderRepository.findById(order.orderId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(paymentEventRepository.findByPaymentIdOrderByIdAsc(payment.paymentId()))
                .extracting(event -> event.getAfterStatus())
                .containsExactly(PaymentStatus.READY, PaymentStatus.APPROVING, PaymentStatus.FAILED);
    }

    @Test
    void cancelApprovedPaymentRefundsWallet() {
        long beforeBalance = walletBalance();
        OrderResponse order = orderService.create(new CreateOrderRequest(DEMO_USER_ID, 7_000L));
        PaymentResponse approved = paymentTransactionService.approve(
                new ApprovePaymentRequest(order.orderId(), "approve-key-3")
        );

        PaymentResponse canceled = paymentTransactionService.cancel(
                approved.paymentId(),
                new CancelPaymentRequest("테스트 취소")
        );

        assertThat(canceled.status()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(walletBalance()).isEqualTo(beforeBalance);
        assertThat(orderRepository.findById(order.orderId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CANCELED);
        assertThat(paymentEventRepository.findByPaymentIdOrderByIdAsc(approved.paymentId()))
                .extracting(event -> event.getAfterStatus())
                .containsExactly(
                        PaymentStatus.READY,
                        PaymentStatus.APPROVING,
                        PaymentStatus.APPROVED,
                        PaymentStatus.CANCELING,
                        PaymentStatus.CANCELED
                );
    }

    @Test
    void settledPaymentCannotBeCanceled() {
        OrderResponse order = orderService.create(new CreateOrderRequest(DEMO_USER_ID, 3_000L));
        PaymentResponse approved = paymentTransactionService.approve(
                new ApprovePaymentRequest(order.orderId(), "approve-key-4")
        );
        paymentTransactionService.settleApprovedPayments();

        assertThatThrownBy(() -> paymentTransactionService.cancel(
                approved.paymentId(),
                new CancelPaymentRequest("정산 후 취소 시도")
        )).isInstanceOf(InvalidPaymentStateException.class);
    }

    private long walletBalance() {
        return walletRepository.findByUserId(DEMO_USER_ID)
                .orElseThrow()
                .getBalance();
    }
}
