package com.pointpay.guard.api;

import com.pointpay.guard.api.dto.SettlementResponse;
import com.pointpay.guard.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

    private final PaymentService paymentService;

    public SettlementController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/run")
    public ResponseEntity<SettlementResponse> settle() {
        return ResponseEntity.ok(paymentService.settleApprovedPayments());
    }
}
