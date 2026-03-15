package com.juno.healthapp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OnboardingSurveyResponse(
        UUID id,
        BigDecimal heightCm,
        BigDecimal weightKg,
        BigDecimal waistCircumferenceCm,
        String pregnancyStatus,
        Boolean conditionsNone,
        List<String> conditionsSelected,
        String conditionsOtherText,
        List<String> surgicalHistory,
        List<Object> medications,
        List<Object> allergies,
        Object familyHistory,
        String smokingStatus,
        String alcoholFrequency,
        String physicalActivityLevel,
        String dietPattern,
        String sleepPattern,
        String lifestyleNotes,
        boolean completed,
        LocalDateTime completedAt,
        LocalDateTime lastUpdatedAt
) {}