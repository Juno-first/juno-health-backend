package com.juno.healthapp.dto;

import java.util.List;

public record RouteInfo(
        double distanceMeters,
        double durationSeconds,
        double durationMinutes,
        List<String> steps,
        List<List<double[]>> geometry   // MultiLineString coordinates
) {}