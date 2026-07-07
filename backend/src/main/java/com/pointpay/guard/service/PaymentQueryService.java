package com.pointpay.guard.service;

import com.pointpay.guard.api.dto.PaymentEventResponse;
import com.pointpay.guard.api.dto.PaymentResponse;
import com.pointpay.guard.api.dto.PaymentStatusSummaryResponse;
import com.pointpay.guard.api.dto.PaymentSummaryResponse;
import com.pointpay.guard.domain.payment.PaymentStatus;
import com.pointpay.guard.global.exception.ErrorCode;
import com.pointpay.guard.global.exception.ResourceNotFoundException;
import com.pointpay.guard.repository.PaymentEventRepository;
import com.pointpay.guard.repository.PaymentRepository;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentQueryService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;

    public PaymentQueryService(PaymentRepository paymentRepository, PaymentEventRepository paymentEventRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentEventRepository = paymentEventRepository;
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long paymentId) {
        return paymentRepository.findDetailById(paymentId)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.PAYMENT_NOT_FOUND,
                        "결제 정보를 찾을 수 없습니다. paymentId=" + paymentId
                ));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByIdempotencyKey(String idempotencyKey) {
        return paymentRepository.findDetailByIdempotencyKey(idempotencyKey)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.PAYMENT_NOT_FOUND,
                        "멱등성 키에 해당하는 결제 결과가 아직 없습니다. idempotencyKey=" + idempotencyKey
                ));
    }

    @Transactional(readOnly = true)
    public List<PaymentEventResponse> getPaymentEvents(Long paymentId) {
        if (!paymentRepository.existsById(paymentId)) {
            throw new ResourceNotFoundException(
                    ErrorCode.PAYMENT_NOT_FOUND,
                    "결제 정보를 찾을 수 없습니다. paymentId=" + paymentId
            );
        }
        return paymentEventRepository.findByPaymentIdOrderByIdAsc(paymentId).stream()
                .map(PaymentEventResponse::from)
                .toList();
    }

    /**
     * 전체 결제를 상태별로 집계하고 SETTLED 상태를 누적 정산 결과로 함께 반환한다.
     */
    @Transactional(readOnly = true)
    public PaymentSummaryResponse getPaymentSummary() {
        // 결제가 없는 상태도 모든 PaymentStatus가 0건으로 응답되도록 기본 집계를 먼저 만든다.
        Map<PaymentStatus, PaymentStatusSummaryResponse> summariesByStatus = new EnumMap<>(PaymentStatus.class);
        for (PaymentStatus status : PaymentStatus.values()) {
            summariesByStatus.put(status, new PaymentStatusSummaryResponse(status, 0L, 0L));
        }

        // DB GROUP BY 결과가 존재하는 상태만 실제 건수와 금액으로 교체한다.
        for (PaymentRepository.PaymentStatusAggregate aggregate : paymentRepository.summarizeByStatus()) {
            summariesByStatus.put(aggregate.getStatus(), new PaymentStatusSummaryResponse(
                    aggregate.getStatus(),
                    aggregate.getPaymentCount(),
                    aggregate.getTotalAmount()
            ));
        }

        // enum 선언 순서를 유지해 클라이언트가 항상 동일한 상태 목록을 받을 수 있게 한다.
        List<PaymentStatusSummaryResponse> statusSummaries = Arrays.stream(PaymentStatus.values())
                .map(summariesByStatus::get)
                .toList();

        // 전체 결제 값은 각 상태 집계의 합으로 계산해 별도의 추가 쿼리를 발생시키지 않는다.
        long totalPaymentCount = statusSummaries.stream()
                .mapToLong(PaymentStatusSummaryResponse::paymentCount)
                .sum();
        long totalPaymentAmount = statusSummaries.stream()
                .mapToLong(PaymentStatusSummaryResponse::totalAmount)
                .sum();

        // 별도 정산 이력 엔티티가 없으므로 현재 SETTLED 상태인 결제를 누적 정산 결과로 사용한다.
        PaymentStatusSummaryResponse settledSummary = summariesByStatus.get(PaymentStatus.SETTLED);
        return new PaymentSummaryResponse(
                totalPaymentCount,
                totalPaymentAmount,
                settledSummary.paymentCount(),
                settledSummary.totalAmount(),
                statusSummaries
        );
    }
}
