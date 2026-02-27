package com.example.paymentservice.service;

import com.example.grpc.notifications.NotificationServiceGrpc;
import com.example.grpc.notifications.NotificationRequest;
import com.example.paymentservice.controller.PaymentController;
import com.example.paymentservice.entity.OutboxEvent;
import com.example.paymentservice.entity.OutboxStatus;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.repositpry.OutboxEventRepository;
import com.example.paymentservice.repositpry.PaymentRepository;
import io.grpc.StatusRuntimeException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class PaymentService {

    final PaymentRepository paymentRepository;
    final OutboxEventRepository outboxRepository;

    @GrpcClient("notification-service")
    private NotificationServiceGrpc.NotificationServiceBlockingStub notificationStub;

    @Transactional
    public Payment createPayment(Payment.CreateRequest request) {
        var payment = Payment.builder()
                .userId(request.userId())
                .amount(request.amount())
                .status(PaymentStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        payment = paymentRepository.save(payment);

        var outboxEvent = OutboxEvent.builder()
                .aggregateType("payment")
                .aggregateId(payment.getId())
                .eventType("PaymentCreated")
                .status(OutboxStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        Map<String, Object> payload = Map.of(
                "user_id", request.userId(),
                "amount", request.amount(),
                "message", "Payment of $" + request.amount() + " is being processed"
        );

        outboxEvent.setPayload(payload);
        outboxRepository.save(outboxEvent);

        log.info("Created payment {} and outbox event", payment.getId());

        return payment;
    }

    @Transactional
    public void processOutboxEvents(int batchSize) {
        var events = outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, PageRequest.of(0, batchSize));

        for (var event : events) {
            try {
                processSingleEvent(event);
            } catch (Exception e) {
                log.error("Failed to process outbox event: {}", event.getId(), e);
            }
        }
    }

    @Transactional(readOnly = true)
    public PaymentController.PaymentStatusResponse getStatus(Long id) {
        var payment = paymentRepository.getReferenceById(id);
        return new PaymentController.PaymentStatusResponse(payment.getId(), payment.getStatus().toString());
    }

    private void processSingleEvent(OutboxEvent event) {
        log.info("Processing outbox event: {}", event.getId());

        try {
            var request = NotificationRequest.newBuilder()
                    .setPaymentId(event.getAggregateId().toString())
                    .setUserId((Integer) event.getPayload().get("user_id"))
                    .setAmount((Double) event.getPayload().get("amount"))
                    .setMessage((String) event.getPayload().get("message"))
                    .build();

            var response = notificationStub.sendNotification(request);

            if (response.getSuccess()) {
                event.setStatus(OutboxStatus.SENT);
                event.setProcessedAt(Instant.now());
                outboxRepository.save(event);
                log.info("Successfully processed outbox event: {}", event.getId());
            } else {
                event.setStatus(OutboxStatus.FAILED);
                outboxRepository.save(event);
                log.error("Failed to send notification for event: {}", event.getId());
            }

        } catch (StatusRuntimeException e) {
            log.error("gRPC error for event {}: {}", event.getId(), e.getStatus());
        }
    }
}
