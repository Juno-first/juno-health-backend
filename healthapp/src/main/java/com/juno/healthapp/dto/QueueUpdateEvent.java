package com.juno.healthapp.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonFormat;

public record QueueUpdateEvent(
        UUID departmentId,
        UUID patientId,
        UUID visitId,
        UUID queueEntryId,
        String patientName,
        int position,
        int queueDepth,
        String priorityTier,
        int aiPriorityScore,
        int estimatedWaitMinutes,
        String symptomSeverity,
        Integer painLevel,
        List<String> symptomCategories,
        String symptomDuration,
        String presentingComplaint,
        String additionalNotes,
        String status,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
        LocalDateTime checkedInAt,
        String roomName,
        String assignedStaffName,
        String assignedStaffRole,
        String eventType,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", shape = JsonFormat.Shape.STRING)
        LocalDateTime eventTimestamp  // ← added
) {}