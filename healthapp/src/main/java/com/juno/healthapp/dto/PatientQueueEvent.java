package com.juno.healthapp.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PatientQueueEvent(
    UUID patientId,
    UUID departmentId,
    UUID visitId,
    String queueStatus, // waiting, being_assessed, processed, cancelled
    Integer priorityLevel,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String notes
) {
    public PatientQueueEvent {
        if (queueStatus == null) {
            queueStatus = "waiting";
        }
        if (priorityLevel == null) {
            priorityLevel = 3; // default priority
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
}
