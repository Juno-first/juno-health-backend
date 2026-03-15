package com.juno.healthapp.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CheckInResponse(
        UUID visitId,
        UUID queueEntryId,
        int position,
        int queueDepth,
        String priorityTier,
        int aiPriorityScore,
        int estimatedWaitMinutes,
        String facilityName,
        String departmentName,
        String status,
        String qrToken,
        String checkinCode,
        LocalDateTime checkedInAt,
        String roomName,
        String assignedStaffName,
        String assignedStaffRole
) {}