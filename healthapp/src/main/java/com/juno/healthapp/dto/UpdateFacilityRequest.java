package com.juno.healthapp.dto;


import java.math.BigDecimal;

public record UpdateFacilityRequest(
        String name,
        String description,
        String facilityType,
        String address,
        String parish,
        BigDecimal latitude,
        BigDecimal longitude,
        String phone,
        Boolean nhfAccepted,
        Boolean pulsejaRegistered
) {}