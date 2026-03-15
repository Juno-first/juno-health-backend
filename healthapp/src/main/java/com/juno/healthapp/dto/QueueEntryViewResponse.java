package com.juno.healthapp.dto;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record QueueEntryViewResponse(
        UUID queueEntryId,
        UUID visitId,
        UUID patientId,
        String patientName,
        int age,
        String departmentName,
        int position,
        int aiPriorityScore,
        String priorityTier,
        String symptomSeverity,
        Integer painLevel,
        List<String> symptomCategories,
        String symptomDuration,
        String presentingComplaint,
        String additionalNotes,
        String status,
        LocalDateTime checkedInAt
) {}