package com.juno.healthapp.service;

import com.juno.healthapp.dao.OnboardingSurveyDAO;
import com.juno.healthapp.dao.PatientDAO;
import com.juno.healthapp.dto.OnboardingSurveyRequest;
import com.juno.healthapp.dto.OnboardingSurveyResponse;
import com.juno.healthapp.entity.OnboardingSurvey;
import com.juno.healthapp.entity.Patient;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OnboardingSurveyService {

    private final OnboardingSurveyDAO onboardingSurveyDAO;
    private final PatientDAO patientDAO;

    /**
     * Save or update — only overwrites fields that are non-null in the request.
     * This allows the frontend to send only the current step's data.
     */
    @Transactional
    public OnboardingSurveyResponse saveProgress(UUID patientId, OnboardingSurveyRequest request) {
        Patient patient = patientDAO.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

        // Load existing or create new
        OnboardingSurvey survey = onboardingSurveyDAO.findByPatient(patient)
                .orElseGet(() -> OnboardingSurvey.builder()
                        .patient(patient)
                        .completed(false)
                        .build());

        // ── Demographics ──────────────────────────────────────────────────
        if (request.heightCm() != null)            survey.setHeightCm(request.heightCm());
        if (request.weightKg() != null)            survey.setWeightKg(request.weightKg());
        if (request.waistCircumferenceCm() != null) survey.setWaistCircumferenceCm(request.waistCircumferenceCm());
        if (request.pregnancyStatus() != null)     survey.setPregnancyStatus(request.pregnancyStatus());

        // ── Conditions ────────────────────────────────────────────────────
        if (request.conditionsNone() != null)      survey.setConditionsNone(request.conditionsNone());
        if (request.conditionsSelected() != null)  survey.setConditionsSelected(request.conditionsSelected());
        if (request.conditionsOtherText() != null) survey.setConditionsOtherText(request.conditionsOtherText());
        if (request.surgicalHistory() != null)     survey.setSurgicalHistory(request.surgicalHistory());

        // ── Medications ───────────────────────────────────────────────────
        if (request.medications() != null)         survey.setMedications(request.medications());

        // ── Allergies ─────────────────────────────────────────────────────
        if (request.allergies() != null)           survey.setAllergies(request.allergies());

        // ── Family History ────────────────────────────────────────────────
        if (request.familyHistory() != null)       survey.setFamilyHistory(request.familyHistory());

        // ── Lifestyle ─────────────────────────────────────────────────────
        if (request.smokingStatus() != null)          survey.setSmokingStatus(request.smokingStatus());
        if (request.alcoholFrequency() != null)       survey.setAlcoholFrequency(request.alcoholFrequency());
        if (request.physicalActivityLevel() != null)  survey.setPhysicalActivityLevel(request.physicalActivityLevel());
        if (request.dietPattern() != null)            survey.setDietPattern(request.dietPattern());
        if (request.sleepPattern() != null)           survey.setSleepPattern(request.sleepPattern());
        if (request.lifestyleNotes() != null)         survey.setLifestyleNotes(request.lifestyleNotes());

        // ── Completion ────────────────────────────────────────────────────
        if (request.markCompleted() && !survey.isCompleted()) {
            survey.setCompleted(true);
            survey.setCompletedAt(LocalDateTime.now());
        }

        survey.setLastUpdatedAt(LocalDateTime.now());

        return toResponse(onboardingSurveyDAO.save(survey));
    }

    /**
     * Fetch current progress — frontend uses this to pre-populate the form on resume.
     */
    public OnboardingSurveyResponse getProgress(UUID patientId) {
        Patient patient = patientDAO.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

        return onboardingSurveyDAO.findByPatient(patient)
                .map(this::toResponse)
                .orElse(null);
    }

    private OnboardingSurveyResponse toResponse(OnboardingSurvey s) {
        return new OnboardingSurveyResponse(
                s.getId(),
                s.getHeightCm(),
                s.getWeightKg(),
                s.getWaistCircumferenceCm(),
                s.getPregnancyStatus(),
                s.isConditionsNone(),
                s.getConditionsSelected(),
                s.getConditionsOtherText(),
                s.getSurgicalHistory(),
                s.getMedications(),
                s.getAllergies(),
                s.getFamilyHistory(),
                s.getSmokingStatus(),
                s.getAlcoholFrequency(),
                s.getPhysicalActivityLevel(),
                s.getDietPattern(),
                s.getSleepPattern(),
                s.getLifestyleNotes(),
                s.isCompleted(),
                s.getCompletedAt(),
                s.getLastUpdatedAt()
        );
    }
}