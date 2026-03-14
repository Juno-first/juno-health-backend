package com.juno.healthapp.dto;


import java.util.UUID;

public record StaffRegisterRequest(
        String firstName,
        String middleName,
        String lastName,
        String email,
        String phone,
        String password,
        String role,
        String specialty,
        UUID facilityId,
        UUID departmentId
) {}