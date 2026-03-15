package com.juno.healthapp.dto;

import java.math.BigDecimal;
import java.util.List;

// Each section is optional so the patient can save partially
public record OnboardingSurveyRequest(
        // Demographics
        BigDecimal heightCm,
        BigDecimal weightKg,
        BigDecimal waistCircumferenceCm,
        String pregnancyStatus,

        // Conditions
        Boolean conditionsNone,
        List<String> conditionsSelected,
        String conditionsOtherText,
        List<String> surgicalHistory,

        // Medications
        List<Object> medications,

        // Allergies
        List<Object> allergies,

        // Family history
        Object familyHistory,

        // Lifestyle
        String smokingStatus,
        String alcoholFrequency,
        String physicalActivityLevel,
        String dietPattern,
        String sleepPattern,
        String lifestyleNotes,

        // Whether to mark as fully complete
        boolean markCompleted
) {}