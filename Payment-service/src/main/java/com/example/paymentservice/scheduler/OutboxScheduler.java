package com.example.paymentservice.scheduler;

import com.example.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxScheduler {

    private final PaymentService paymentService;

    @Scheduled(fixedDelayString = "${outbox.processor.fixed-delay:5000}")
    public void processOutboxEvents() {
        log.debug("Starting outbox events processing");
        paymentService.processOutboxEvents(10);
        log.debug("Finished outbox events processing");
    }
}
