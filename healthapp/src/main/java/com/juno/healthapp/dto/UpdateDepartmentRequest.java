package com.juno.healthapp.dto;

public record UpdateDepartmentRequest(
        String name,
        String description,
        String departmentType,
        Integer capacity,
        Boolean isActive
) {}