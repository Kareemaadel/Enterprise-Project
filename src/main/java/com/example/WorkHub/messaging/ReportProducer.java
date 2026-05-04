package com.example.WorkHub.messaging;

import com.example.WorkHub.config.KafkaConfig;
import com.example.WorkHub.dto.ReportRequestEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka producer that publishes report generation request events.
 * Uses the jobId as the Kafka message key for consistent partition routing
 * and downstream idempotency.
 */
@Component
public class ReportProducer {

    private static final Logger logger = LoggerFactory.getLogger(ReportProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public ReportProducer(KafkaTemplate<String, String> kafkaTemplate,
                          ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publishes a report request event to the Kafka "report-requests" topic.
     *
     * @param event the report request event containing jobId, projectId, tenantId, and requester info
     */
    public void sendReportRequest(ReportRequestEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String key = event.jobId().toString();

            kafkaTemplate.send(KafkaConfig.REPORT_TOPIC, key, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            logger.error("Failed to send report request for job {}: {}",
                                    event.jobId(), ex.getMessage());
                        } else {
                            logger.info("Report request published for job {} to partition {} at offset {}",
                                    event.jobId(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize ReportRequestEvent: {}", e.getMessage());
            throw new RuntimeException("Failed to serialize report request event", e);
        }
    }
}
