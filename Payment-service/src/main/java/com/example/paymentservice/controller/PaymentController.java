package com.example.paymentservice.controller;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<Payment.Response> createPayment(@RequestBody Payment.CreateRequest request) {
        var payment = paymentService.createPayment(request);
        var response = new Payment.Response(payment.getId(), "Payment accepted for processing");

        return ResponseEntity
                .accepted()
                .location(URI.create("/api/v1/payments/" + payment.getId()))
                .body(response);
    }

    @GetMapping("/{paymentId}/status")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(@PathVariable Long paymentId) {
        return ResponseEntity.ok(new PaymentStatusResponse(paymentId, "PENDING"));
    }

    public record PaymentStatusResponse(Long paymentId, String status) {}
}
