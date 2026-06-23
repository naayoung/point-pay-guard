package com.pointpay.guard.api;

import com.pointpay.guard.api.dto.ApprovePaymentRequest;
import com.pointpay.guard.api.dto.CancelPaymentRequest;
import com.pointpay.guard.api.dto.PaymentEventResponse;
import com.pointpay.guard.api.dto.PaymentResponse;
import com.pointpay.guard.service.PaymentQueryService;
import com.pointpay.guard.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentQueryService paymentQueryService;

    public PaymentController(PaymentService paymentService, PaymentQueryService paymentQueryService) {
        this.paymentService = paymentService;
        this.paymentQueryService = paymentQueryService;
    }

    @PostMapping("/approve")
    public ResponseEntity<PaymentResponse> approve(@Valid @RequestBody ApprovePaymentRequest request) {
        return ResponseEntity.ok(paymentService.approve(request));
    }

    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<PaymentResponse> cancel(
            @PathVariable Long paymentId,
            @RequestBody(required = false) CancelPaymentRequest request
    ) {
        CancelPaymentRequest cancelRequest = request == null ? new CancelPaymentRequest(null) : request;
        return ResponseEntity.ok(paymentService.cancel(paymentId, cancelRequest));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentQueryService.getPayment(paymentId));
    }

    @GetMapping("/{paymentId}/events")
    public ResponseEntity<List<PaymentEventResponse>> getPaymentEvents(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentQueryService.getPaymentEvents(paymentId));
    }
}
