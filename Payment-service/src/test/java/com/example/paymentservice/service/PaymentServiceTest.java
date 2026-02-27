package com.example.paymentservice.service;

import com.example.grpc.notifications.NotificationRequest;
import com.example.grpc.notifications.NotificationServiceGrpc;
import com.example.paymentservice.entity.OutboxEvent;
import com.example.paymentservice.entity.OutboxStatus;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.repositpry.OutboxEventRepository;
import com.example.paymentservice.repositpry.PaymentRepository;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PaymentService.class)
class PaymentServiceTest {

    @Autowired
    PaymentService paymentService;
    @MockitoBean
    PaymentRepository paymentRepository;
    @MockitoBean
    OutboxEventRepository outboxRepository;
    @MockitoBean
    NotificationServiceGrpc.NotificationServiceBlockingStub notificationStub;


    @Captor
    ArgumentCaptor<Payment> paymentCaptor;
    @Captor
    ArgumentCaptor<OutboxEvent> outboxCaptor;

    Payment.CreateRequest validRequest;
    Payment savedPayment;
    OutboxEvent outboxEvent;

    @BeforeEach
    void setUp() {
        validRequest = new Payment.CreateRequest(123L, new BigDecimal("99.99"));

        savedPayment = Payment.builder()
                .id(1L)
                .userId(123L)
                .amount(new BigDecimal("99.99"))
                .status(PaymentStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        outboxEvent = OutboxEvent.builder()
                .id(1L)
                .aggregateType("payment")
                .aggregateId(1L)
                .eventType("PaymentCreated")
                .status(OutboxStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        Map<String, Object> payload = Map.of(
                "user_id", 123,
                "amount", 99.99,
                "message", "Payment of $99.99 is being processed"
        );
        outboxEvent.setPayload(payload);
    }

    @Test
    void createPaymentTest() {
        given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment);
        given(outboxRepository.save(any(OutboxEvent.class))).willReturn(outboxEvent);

        Payment result = paymentService.createPayment(validRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(123L);
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);

        then(paymentRepository).should(times(1)).save(paymentCaptor.capture());
        Payment capturedPayment = paymentCaptor.getValue();
        assertThat(capturedPayment.getUserId()).isEqualTo(123L);
        assertThat(capturedPayment.getAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(capturedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(capturedPayment.getCreatedAt()).isNotNull();

        then(outboxRepository).should(times(1)).save(outboxCaptor.capture());
        OutboxEvent capturedOutbox = outboxCaptor.getValue();
        assertThat(capturedOutbox.getAggregateType()).isEqualTo("payment");
        assertThat(capturedOutbox.getAggregateId()).isEqualTo(savedPayment.getId());
        assertThat(capturedOutbox.getEventType()).isEqualTo("PaymentCreated");
        assertThat(capturedOutbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(capturedOutbox.getCreatedAt()).isNotNull();

        Map<String, Object> payload = capturedOutbox.getPayload();
        assertThat(payload).containsEntry("user_id", 123L);
        assertThat(payload).containsEntry("amount", new BigDecimal("99.99"));
        assertThat(payload).containsKey("message");
    }

    @Test
    void createPaymentWithExceptionTest() {
        given(paymentRepository.save(any(Payment.class))).willThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> paymentService.createPayment(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error");

        then(paymentRepository).should(times(1)).save(any(Payment.class));

        then(outboxRepository).should(never()).save(any(OutboxEvent.class));
    }

    @Test
    void processOutboxEventsWithExceptionTest() {
        List<OutboxEvent> pendingEvents = List.of(outboxEvent);
        given(outboxRepository.findByStatusOrderByCreatedAtAsc(
                eq(OutboxStatus.PENDING),
                any(PageRequest.class)))
                .willReturn(pendingEvents);

        given(notificationStub.sendNotification(any(NotificationRequest.class))).willThrow(mock(StatusRuntimeException.class));

        paymentService.processOutboxEvents(10);

        then(outboxRepository).should(never()).save(any(OutboxEvent.class));
    }

    @Test
    void getStatusTest() {
        Long paymentId = 1L;
        given(paymentRepository.getReferenceById(paymentId)).willReturn(savedPayment);

        var result = paymentService.getStatus(paymentId);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("PENDING");

        then(paymentRepository).should(times(1)).getReferenceById(paymentId);
    }

    @Test
    void getStatusWithExceptionTest() {
        Long paymentId = 999L;
        given(paymentRepository.getReferenceById(paymentId)).willThrow(new jakarta.persistence.EntityNotFoundException());

        assertThatThrownBy(() -> paymentService.getStatus(paymentId)).isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }
}