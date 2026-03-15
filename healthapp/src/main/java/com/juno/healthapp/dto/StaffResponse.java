package com.juno.healthapp.dto;

import java.util.UUID;

public record StaffResponse(
        UUID id,
        String fullName,
        String role,
        String specialty,
        boolean isOnDuty
) {}