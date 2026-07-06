package com.pointpay.guard.api;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.pointpay.guard.api.dto.CreateOrderRequest;
import com.pointpay.guard.api.dto.OrderResponse;
import com.pointpay.guard.domain.payment.Payment;
import com.pointpay.guard.domain.payment.PaymentStatus;
import com.pointpay.guard.infrastructure.memory.InMemoryIdempotencyGuard;
import com.pointpay.guard.repository.PaymentEventRepository;
import com.pointpay.guard.repository.PaymentRepository;
import com.pointpay.guard.repository.WalletRepository;
import com.pointpay.guard.service.OrderService;
import com.pointpay.guard.service.redis.IdempotencyGuard;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 동일한 Idempotency Key를 가진 승인 요청이 동시에 들어왔을 때
 * 하나의 요청만 결제를 처리하고 나머지 요청은 중복 요청으로 거절되는지 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Import(PaymentIdempotencyConcurrencyIntegrationTest.ConcurrencyTestConfiguration.class)
class PaymentIdempotencyConcurrencyIntegrationTest {

    // 데모 프로필이 애플리케이션 시작 시 생성하는 사용자와 테스트 결제 정보를 사용한다.
    private static final Long DEMO_USER_ID = 1L;
    private static final Long PAYMENT_AMOUNT = 10_000L;
    private static final String IDEMPOTENCY_KEY = "concurrent-approve-key";

    // 실제 HTTP 요청과 동일하게 Controller 진입부터 응답 직렬화까지 검증한다.
    @Autowired
    private MockMvc mockMvc;

    // 동시 승인 요청의 선행 조건인 주문을 생성한다.
    @Autowired
    private OrderService orderService;

    // 결제 전후 잔액을 비교해 포인트가 한 번만 차감되었는지 확인한다.
    @Autowired
    private WalletRepository walletRepository;

    // 동일한 멱등성 키로 결제가 한 건만 생성되었는지 확인한다.
    @Autowired
    private PaymentRepository paymentRepository;

    // 승인 상태 전이 이벤트가 중복 저장되지 않았는지 확인한다.
    @Autowired
    private PaymentEventRepository paymentEventRepository;

    // 첫 번째 요청의 키 선점 시점을 제어해 두 요청의 경합을 재현한다.
    @Autowired
    private CoordinatedIdempotencyGuard idempotencyGuard;

    @Test
    void concurrentRequestsWithSameIdempotencyKeyProcessPaymentOnlyOnce() throws Exception {
        // 지갑 잔액을 기록하고 두 승인 요청이 함께 사용할 주문을 준비한다.
        long balanceBeforePayment = walletBalance();
        OrderResponse order = orderService.create(new CreateOrderRequest(DEMO_USER_ID, PAYMENT_AMOUNT));

        // 두 작업 스레드가 모두 준비된 뒤 같은 순간에 HTTP 요청을 보내도록 시작 신호를 공유한다.
        CountDownLatch requestsReady = new CountDownLatch(2);
        CountDownLatch startRequests = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // 어떤 요청이 먼저 끝날지 정하지 않고 완료된 응답부터 검증한다.
        ExecutorCompletionService<MvcResult> responses = new ExecutorCompletionService<>(executor);

        try {
            // 요청 내용과 Idempotency Key가 완전히 같은 승인 요청 두 개를 별도 스레드에 등록한다.
            Future<MvcResult> firstRequest = responses.submit(
                    () -> approve(order.orderId(), requestsReady, startRequests)
            );
            Future<MvcResult> secondRequest = responses.submit(
                    () -> approve(order.orderId(), requestsReady, startRequests)
            );

            // 두 스레드가 대기 상태에 도달한 것을 확인한 후 동시에 요청을 시작한다.
            assertThat(requestsReady.await(5, SECONDS)).isTrue();
            startRequests.countDown();

            // 한 요청이 키를 선점해 PROCESSING 상태가 될 때까지 기다린다.
            assertThat(idempotencyGuard.awaitKeyClaimed()).isTrue();

            // 선점 요청을 잠시 멈춘 상태이므로 다른 요청은 처리 중 중복 요청으로 거절되어야 한다.
            MvcResult duplicateResponse = completedResponse(responses);
            assertThat(duplicateResponse.getResponse().getStatus()).isEqualTo(409);
            assertThat(duplicateResponse.getResponse().getContentAsString())
                    .contains("\"code\":\"DUPLICATE_PAYMENT_REQUEST\"");

            // 중복 요청 응답을 확인한 뒤 키를 선점한 요청이 실제 결제를 진행하도록 재개한다.
            idempotencyGuard.allowClaimedRequestToContinue();

            // 선점에 성공한 요청은 정상 승인 결과와 사용한 Idempotency Key를 반환해야 한다.
            MvcResult approvedResponse = completedResponse(responses);
            assertThat(approvedResponse.getResponse().getStatus()).isEqualTo(200);
            assertThat(approvedResponse.getResponse().getContentAsString())
                    .contains("\"status\":\"APPROVED\"")
                    .contains("\"idempotencyKey\":\"" + IDEMPOTENCY_KEY + "\"");
            assertThat(firstRequest.isDone()).isTrue();
            assertThat(secondRequest.isDone()).isTrue();

            // 최종 DB 상태를 확인해 결제 생성과 포인트 차감이 정확히 한 번만 발생했음을 검증한다.
            assertThat(paymentRepository.count()).isEqualTo(1L);
            Payment payment = paymentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY).orElseThrow();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(walletBalance()).isEqualTo(balanceBeforePayment - PAYMENT_AMOUNT);

            // 단일 결제의 정상 승인 과정에서 발생하는 세 개의 상태 이벤트만 저장되어야 한다.
            assertThat(paymentEventRepository.findByPaymentIdOrderByIdAsc(payment.getId()))
                    .extracting(event -> event.getAfterStatus())
                    .containsExactly(PaymentStatus.READY, PaymentStatus.APPROVING, PaymentStatus.APPROVED);
        } finally {
            // assertion 실패 시에도 대기 중인 요청을 깨우고 작업 스레드를 종료해 테스트가 멈추지 않게 한다.
            idempotencyGuard.allowClaimedRequestToContinue();
            executor.shutdownNow();
        }
    }

    /**
     * 시작 신호를 기다린 뒤 결제 승인 API를 호출한다.
     * 두 작업이 요청 직전까지 함께 도달하도록 해 실제 동시 요청을 재현한다.
     */
    private MvcResult approve(
            Long orderId,
            CountDownLatch requestsReady,
            CountDownLatch startRequests
    ) throws Exception {
        // 현재 작업이 요청을 보낼 준비가 되었음을 메인 테스트 스레드에 알린다.
        requestsReady.countDown();
        if (!startRequests.await(5, SECONDS)) {
            throw new IllegalStateException("동시 요청 시작 신호를 기다리는 중 시간 초과가 발생했습니다.");
        }

        // 두 요청 모두 같은 주문 ID와 Idempotency Key를 사용한다.
        return mockMvc.perform(post("/api/payments/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": %d,
                                  "idempotencyKey": "%s"
                                }
                                """.formatted(orderId, IDEMPOTENCY_KEY)))
                .andReturn();
    }

    /**
     * 데모 사용자의 현재 지갑 잔액을 조회한다.
     */
    private long walletBalance() {
        return walletRepository.findByUserId(DEMO_USER_ID)
                .orElseThrow()
                .getBalance();
    }

    /**
     * 무한 대기를 피하기 위해 제한 시간 안에 먼저 완료된 HTTP 응답을 반환한다.
     */
    private MvcResult completedResponse(ExecutorCompletionService<MvcResult> responses) throws Exception {
        Future<MvcResult> response = responses.poll(5, SECONDS);
        assertThat(response).as("5초 안에 완료된 HTTP 응답").isNotNull();
        return response.get();
    }

    /**
     * 운영 코드는 수정하지 않고 테스트에서만 키 선점 시점을 제어하기 위한 Bean을 등록한다.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class ConcurrencyTestConfiguration {

        @Bean
        @Primary
        CoordinatedIdempotencyGuard coordinatedIdempotencyGuard(InMemoryIdempotencyGuard delegate) {
            // PaymentService가 테스트용 가드를 우선 주입받도록 @Primary Bean으로 감싼다.
            return new CoordinatedIdempotencyGuard(delegate);
        }
    }

    /**
     * 실제 메모리 멱등성 가드에 위임하되, 최초 키 선점 요청만 테스트 신호가 올 때까지 대기시킨다.
     */
    static class CoordinatedIdempotencyGuard implements IdempotencyGuard {

        // 실제 putIfAbsent 기반 멱등성 동작은 운영용 InMemoryIdempotencyGuard가 수행한다.
        private final InMemoryIdempotencyGuard delegate;

        // 최초 요청의 키 선점 완료와 해당 요청의 재개 시점을 각각 제어한다.
        private final CountDownLatch keyClaimed = new CountDownLatch(1);
        private final CountDownLatch continueClaimedRequest = new CountDownLatch(1);

        CoordinatedIdempotencyGuard(InMemoryIdempotencyGuard delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean claim(String idempotencyKey) {
            // 실제 가드에서 원자적으로 키를 선점하므로 두 요청 중 하나만 true를 받는다.
            boolean claimed = delegate.claim(idempotencyKey);

            // 최초 요청을 결제 처리 전에 멈춰 두 번째 요청이 반드시 PROCESSING 키를 만나게 한다.
            if (claimed && IDEMPOTENCY_KEY.equals(idempotencyKey)) {
                keyClaimed.countDown();
                awaitContinueSignal();
            }
            return claimed;
        }

        @Override
        public void complete(String idempotencyKey, Long paymentId) {
            // 결제 완료 상태 저장은 실제 가드의 구현을 그대로 사용한다.
            delegate.complete(idempotencyKey, paymentId);
        }

        @Override
        public void release(String idempotencyKey) {
            // 승인 실패 시 재시도할 수 있도록 키를 해제하는 운영 동작을 그대로 사용한다.
            delegate.release(idempotencyKey);
        }

        // 메인 테스트 스레드가 최초 키 선점 완료를 제한 시간 동안 기다린다.
        boolean awaitKeyClaimed() throws InterruptedException {
            return keyClaimed.await(5, SECONDS);
        }

        // 중복 요청 검증이 끝나면 최초 요청이 결제 처리를 계속하도록 신호를 보낸다.
        void allowClaimedRequestToContinue() {
            continueClaimedRequest.countDown();
        }

        // 최초 요청 스레드는 테스트가 재개 신호를 보내거나 제한 시간이 끝날 때까지 대기한다.
        private void awaitContinueSignal() {
            try {
                if (!continueClaimedRequest.await(5, SECONDS)) {
                    throw new IllegalStateException("선점 요청의 재개 신호를 기다리는 중 시간 초과가 발생했습니다.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("멱등성 키 선점 대기 중 인터럽트가 발생했습니다.", e);
            }
        }
    }
}
