package com.juno.healthapp.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


public record QueuePatientSnapshot(
        UUID queueEntryId,
        UUID visitId,
        UUID patientId,
        String patientName,
        int position,
        String priorityTier,
        Integer aiPriorityScore,
        String symptomSeverity,
        Integer painLevel,
        List<String> symptomCategories,
        String symptomDuration,
        String presentingComplaint,
        String additionalNotes,
        String visitStatus,
        LocalDateTime checkedInAt,
        Integer timeInQueueMinutes,
        Integer estimatedWaitMinutes,
        LocalDateTime lastMovedUpAt,
        String roomName,
        String assignedStaffName,
        String assignedStaffRole
) {}
