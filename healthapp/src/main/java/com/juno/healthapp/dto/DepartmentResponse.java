package com.juno.healthapp.dto;

import java.util.UUID;

public record DepartmentResponse(
        UUID id,
        UUID facilityId,
        String facilityName,
        String name,
        String description,
        String departmentType,
        Integer capacity,
        Boolean isActive,
        String qrToken,
        String checkinCode
) {}
