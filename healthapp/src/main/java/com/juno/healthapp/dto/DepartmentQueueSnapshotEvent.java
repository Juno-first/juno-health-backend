package com.juno.healthapp.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DepartmentQueueSnapshotEvent(
        UUID departmentId,
        String facilityName,
        String departmentName,
        String eventType,
        LocalDateTime generatedAt,
        int queueDepth,
        int averageEstimatedWaitMinutes,
        Integer departmentCapacity,
        List<AvailableRoomSnapshot> availableRooms,
        List<AvailableStaffSnapshot> availableStaff,
        List<QueuePatientSnapshot> entries
) {}