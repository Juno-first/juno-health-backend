package com.juno.healthapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.juno.healthapp.dto.PatientQueueEvent;
import com.juno.healthapp.dto.QueueUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import java.nio.charset.StandardCharsets;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientKafkaPublisher {

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE);

    public void sendEvent(String message) {
        kafkaTemplate.send("patient_queue", message);
    }

    public void publishPatientQueueEntry(PatientQueueEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("patient_queue", event.patientId().toString(), payload);
            log.info("Published patient queue event for patient={}", event.patientId());
        } catch (Exception e) {
            log.error("Failed to publish patient queue event", e);
        }
    }

    public void publishPatientQueueEntry(String patientId, Object queueData) {
        try {
            String payload = objectMapper.writeValueAsString(queueData);
            kafkaTemplate.send("patient_queue", patientId, payload);
            log.info("Published patient queue data for patient={}", patientId);
        } catch (Exception e) {
            log.error("Failed to publish patient queue data", e);
        }
    }

    public void publishQueueUpdateEvent(QueueUpdateEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
            kafkaTemplate.send("patient_queue", event.patientId().toString(), payloadBytes);
            log.info("Published detailed queue update event for patient={}, type={}", 
                    event.patientId(), event.eventType());
        } catch (Exception e) {
            log.error("Failed to publish queue update event", e);
        }
    }
}

