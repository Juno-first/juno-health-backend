package com.juno.healthapp.dto;

import java.util.UUID;

public record AvailableRoomSnapshot(
        UUID roomId,
        String roomName,
        String description,
        boolean available,
        UUID assignedStaffId,
        String assignedStaffName
) {}