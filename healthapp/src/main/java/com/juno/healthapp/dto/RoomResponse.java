package com.juno.healthapp.dto;


import java.util.UUID;

public record RoomResponse(
        UUID id,
        String name,
        String description,
        boolean isAvailable,
        UUID assignedStaffId,
        String assignedStaffName
) {}