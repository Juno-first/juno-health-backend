package com.juno.healthapp.dto;

import java.util.List;
import java.util.UUID;

public record CheckInRequest(
        String method,
        String token,

        // Symptom description (the text area)
        String presentingComplaint,

        // Symptom severity card selection
        String symptomSeverity,      // MILD, MODERATE, SEVERE, EMERGENCY

        // Pain slider 0-10
        Integer painLevel,

        // Symptom category checkboxes
        List<String> symptomCategories,// CHEST_PAIN, DIFFICULTY_BREATHING, DIZZINESS, VOMITING, BLEEDING, FEVER, INJURY, ALLERGIC_REACTION

        // Duration dropdown
        String symptomDuration,      // UNDER_1_HOUR, 1_TO_6_HOURS, 6_TO_24_HOURS, OVER_24_HOURS

        // Additional notes
        String additionalNotes,

        String visitType
) {}