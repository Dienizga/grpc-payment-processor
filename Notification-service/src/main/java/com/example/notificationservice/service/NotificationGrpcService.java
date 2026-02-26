package com.example.notificationservice.service;

import com.example.grpc.notifications.NotificationServiceGrpc;
import com.example.grpc.notifications.NotificationRequest;
import com.example.grpc.notifications.NotificationResponse;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@Slf4j
public class NotificationGrpcService extends NotificationServiceGrpc.NotificationServiceImplBase {

    @Override
    public void sendNotification(NotificationRequest request, StreamObserver<NotificationResponse> responseObserver) {

        log.info("Received notification request for payment: {}", request.getPaymentId());

        try {
            sendNotification(request);

            var response = NotificationResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Notification sent successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Failed to send notification", e);

            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription("Failed to send notification: " + e.getMessage())
                            .asRuntimeException()
            );
        }
    }

    private void sendNotification(NotificationRequest request) {
        String channel = switch ((int) (request.getUserId() % 3)) {
            case 0 -> "email";
            case 1 -> "sms";
            case 2 -> "push";
            default -> "unknown";
        };

        log.info("Sending {} notification to user {}: {}", channel, request.getUserId(), request.getMessage());

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while sending notification", e);
        }
    }
}
