package com.juno.healthapp.dto;

import java.util.UUID;

public record FacilityServiceRequest(
        UUID facilityId,
        String name,
        String description
) {}
