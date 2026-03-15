package com.juno.healthapp.dto;


import java.math.BigDecimal;

public record CreateFacilityRequest(
        String name,
        String description,
        String facilityType,
        String address,
        String parish,
        BigDecimal latitude,
        BigDecimal longitude,
        String phone,
        boolean nhfAccepted,
        boolean pulsejaRegistered,
        Object openingHours
) {}