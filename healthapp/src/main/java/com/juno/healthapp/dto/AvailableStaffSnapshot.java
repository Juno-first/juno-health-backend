package com.juno.healthapp.dto;

import java.util.UUID;

public record AvailableStaffSnapshot(
        UUID staffId,
        String fullName,
        String role,
        String specialty,
        boolean onDuty,
        boolean active
) {}
