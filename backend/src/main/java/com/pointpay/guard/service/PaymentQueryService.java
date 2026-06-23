package com.pointpay.guard.service;

import com.pointpay.guard.api.dto.PaymentEventResponse;
import com.pointpay.guard.api.dto.PaymentResponse;
import com.pointpay.guard.global.exception.ErrorCode;
import com.pointpay.guard.global.exception.ResourceNotFoundException;
import com.pointpay.guard.repository.PaymentEventRepository;
import com.pointpay.guard.repository.PaymentRepository;
import java.util.List;
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
}
