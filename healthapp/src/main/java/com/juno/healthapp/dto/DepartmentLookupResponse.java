package com.juno.healthapp.dto;

import java.util.List;

public record DepartmentLookupResponse(
        String facilityName,
        String facilityAddress,
        String departmentName,
        Object openingHours,
        List<FacilityServiceResponse> services
) {}