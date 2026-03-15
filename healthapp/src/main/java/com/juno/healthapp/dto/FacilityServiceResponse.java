package com.juno.healthapp.dto;

import java.util.UUID;

public record FacilityServiceResponse(
        UUID id,
        String name,
        String description
) {}
