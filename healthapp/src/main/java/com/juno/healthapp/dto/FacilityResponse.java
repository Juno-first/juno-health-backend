package com.juno.healthapp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FacilityResponse(
        UUID id,
        String name,
        String description,
        String facilityType,
        String address,
        String parish,
        BigDecimal latitude,
        BigDecimal longitude,
        String phone,
        Boolean nhfAccepted,
        String checkinCode,
        String qrToken,
        LocalDateTime createdAt,
        Integer avgWaitMinutes,
        List<FacilityServiceResponse> services,
        Double distanceKm,
        RouteInfo route
) {}