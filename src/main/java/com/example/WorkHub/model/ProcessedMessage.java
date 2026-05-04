package com.example.WorkHub.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Idempotency table: tracks Kafka messages that have already been processed.
 * Before processing a consumed message, the consumer checks if a record with
 * the same messageKey already exists. If it does, the message is skipped
 * (safe retry / exactly-once semantics at application level).
 */
@Entity
@Table(name = "processed_message")
public class ProcessedMessage {

    @Id
    @Column(nullable = false, unique = true)
    private String messageKey;

    @Column(nullable = false)
    private Instant processedAt;

    public ProcessedMessage() {
    }

    public ProcessedMessage(String messageKey) {
        this.messageKey = messageKey;
        this.processedAt = Instant.now();
    }

    public String getMessageKey() {
        return messageKey;
    }

    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}
