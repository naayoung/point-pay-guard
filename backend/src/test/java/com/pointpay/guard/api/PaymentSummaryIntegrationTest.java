package com.pointpay.guard.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pointpay.guard.api.dto.ApprovePaymentRequest;
import com.pointpay.guard.api.dto.CancelPaymentRequest;
import com.pointpay.guard.api.dto.CreateOrderRequest;
import com.pointpay.guard.api.dto.OrderResponse;
import com.pointpay.guard.api.dto.PaymentResponse;
import com.pointpay.guard.repository.WalletRepository;
import com.pointpay.guard.service.OrderService;
import com.pointpay.guard.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 상태가 여러 형태로 저장된 상황에서 전체 결제·정산 집계 API가
 * 상태별 건수와 금액을 정확하게 반환하는지 실제 Spring Context와 DB를 사용해 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Transactional
class PaymentSummaryIntegrationTest {

    // 데모 프로필이 생성하는 사용자와 각 상태를 만들기 위한 결제 금액을 사용한다.
    private static final Long DEMO_USER_ID = 1L;
    private static final Long CANCELED_AMOUNT = 10_000L;
    private static final Long SETTLED_AMOUNT = 20_000L;
    private static final Long APPROVED_AMOUNT = 30_000L;

    // 집계 API를 실제 HTTP 요청 경로로 호출한다.
    @Autowired
    private MockMvc mockMvc;

    // 결제 승인의 선행 데이터인 주문을 생성한다.
    @Autowired
    private OrderService orderService;

    // 승인, 취소, 정산 흐름을 운영 서비스와 동일하게 실행한다.
    @Autowired
    private PaymentService paymentService;

    // 현재 잔액보다 큰 실패 결제를 만들기 위해 지갑 잔액을 조회한다.
    @Autowired
    private WalletRepository walletRepository;

    @Test
    void getPaymentSummaryAggregatesAllPaymentStatusesAndSettlements() throws Exception {
        // 승인 후 취소한 결제를 만들어 CANCELED 상태 집계 데이터를 준비한다.
        PaymentResponse canceledPayment = approve(CANCELED_AMOUNT, "summary-canceled-key");
        paymentService.cancel(canceledPayment.paymentId(), new CancelPaymentRequest("집계 API 테스트 취소"));

        // 승인 결제를 정산해 SETTLED 상태와 누적 정산 금액을 준비한다.
        approve(SETTLED_AMOUNT, "summary-settled-key");
        paymentService.settleApprovedPayments();

        // 정산 실행 이후 새 결제를 승인해 현재 APPROVED 상태도 한 건 유지한다.
        approve(APPROVED_AMOUNT, "summary-approved-key");

        // 현재 잔액보다 1포인트 큰 결제로 FAILED 상태 집계 데이터를 준비한다.
        long failedAmount = walletBalance() + 1L;
        approve(failedAmount, "summary-failed-key");

        // 모든 상태의 결제 금액을 더한 값이 전체 결제 요청 금액과 일치해야 한다.
        long expectedTotalAmount = CANCELED_AMOUNT + SETTLED_AMOUNT + APPROVED_AMOUNT + failedAmount;

        // 전체 합계, 누적 정산 합계, 각 상태별 0건 포함 집계를 한 응답에서 검증한다.
        mockMvc.perform(get("/api/payments/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPaymentCount").value(4))
                .andExpect(jsonPath("$.totalPaymentAmount").value(expectedTotalAmount))
                .andExpect(jsonPath("$.settledPaymentCount").value(1))
                .andExpect(jsonPath("$.settledPaymentAmount").value(SETTLED_AMOUNT))
                .andExpect(jsonPath("$.statusSummaries.length()").value(7))
                .andExpect(jsonPath("$.statusSummaries[0].status").value("READY"))
                .andExpect(jsonPath("$.statusSummaries[0].paymentCount").value(0))
                .andExpect(jsonPath("$.statusSummaries[2].status").value("APPROVED"))
                .andExpect(jsonPath("$.statusSummaries[2].paymentCount").value(1))
                .andExpect(jsonPath("$.statusSummaries[2].totalAmount").value(APPROVED_AMOUNT))
                .andExpect(jsonPath("$.statusSummaries[3].status").value("FAILED"))
                .andExpect(jsonPath("$.statusSummaries[3].paymentCount").value(1))
                .andExpect(jsonPath("$.statusSummaries[3].totalAmount").value(failedAmount))
                .andExpect(jsonPath("$.statusSummaries[5].status").value("CANCELED"))
                .andExpect(jsonPath("$.statusSummaries[5].paymentCount").value(1))
                .andExpect(jsonPath("$.statusSummaries[5].totalAmount").value(CANCELED_AMOUNT))
                .andExpect(jsonPath("$.statusSummaries[6].status").value("SETTLED"))
                .andExpect(jsonPath("$.statusSummaries[6].paymentCount").value(1))
                .andExpect(jsonPath("$.statusSummaries[6].totalAmount").value(SETTLED_AMOUNT));
    }

    /**
     * 지정한 금액의 주문을 생성하고 서로 다른 Idempotency Key로 결제를 승인한다.
     */
    private PaymentResponse approve(Long amount, String idempotencyKey) {
        OrderResponse order = orderService.create(new CreateOrderRequest(DEMO_USER_ID, amount));
        return paymentService.approve(new ApprovePaymentRequest(order.orderId(), idempotencyKey));
    }

    /**
     * 데모 사용자의 현재 포인트 잔액을 반환한다.
     */
    private long walletBalance() {
        return walletRepository.findByUserId(DEMO_USER_ID)
                .orElseThrow()
                .getBalance();
    }
}
