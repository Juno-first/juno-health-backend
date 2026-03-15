package com.juno.healthapp.dto;

import java.util.UUID;

public record CreateDepartmentRequest(
        UUID facilityId,
        String name,
        String description,
        String departmentType,
        Integer capacity
) {}
