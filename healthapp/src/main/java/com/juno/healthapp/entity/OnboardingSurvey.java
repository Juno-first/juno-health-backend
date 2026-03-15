package com.juno.healthapp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "onboarding_surveys")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingSurvey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", unique = true)
    private Patient patient;

    // ── Demographics ──────────────────────────────────────────────────────────
    @Column(name = "height_cm")
    private BigDecimal heightCm;

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    @Column(name = "waist_circumference_cm")
    private BigDecimal waistCircumferenceCm;

    @Column(name = "pregnancy_status")
    private String pregnancyStatus;

    // ── Chronic Conditions ────────────────────────────────────────────────────
    @Column(name = "conditions_none")
    private boolean conditionsNone = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditions_selected", columnDefinition = "jsonb")
    private List<String> conditionsSelected;

    @Column(name = "conditions_other_text", columnDefinition = "TEXT")
    private String conditionsOtherText;

    // ── Surgical History ──────────────────────────────────────────────────────
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "surgical_history", columnDefinition = "jsonb")
    private List<String> surgicalHistory;

    // ── Medications ───────────────────────────────────────────────────────────
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "medications", columnDefinition = "jsonb")
    private List<Object> medications;

    // ── Allergies ─────────────────────────────────────────────────────────────
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allergies", columnDefinition = "jsonb")
    private List<Object> allergies;

    // ── Family History ────────────────────────────────────────────────────────
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "family_history", columnDefinition = "jsonb")
    private Object familyHistory;

    // ── Lifestyle ─────────────────────────────────────────────────────────────
    @Column(name = "smoking_status")
    private String smokingStatus;

    @Column(name = "alcohol_frequency")
    private String alcoholFrequency;

    @Column(name = "physical_activity_level")
    private String physicalActivityLevel;

    @Column(name = "diet_pattern")
    private String dietPattern;

    @Column(name = "sleep_pattern")
    private String sleepPattern;

    @Column(name = "lifestyle_notes", columnDefinition = "TEXT")
    private String lifestyleNotes;

    // ── Meta ──────────────────────────────────────────────────────────────────
    @Column(name = "completed")
    private boolean completed = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;
}