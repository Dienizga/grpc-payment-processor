package com.example.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "outbox")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "aggregate_type", nullable = false)
    String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    Long aggregateId;

    @Column(name = "event_type", nullable = false)
    String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    OutboxStatus status;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "processed_at")
    Instant processedAt;

    @Version
    Long version;

}
