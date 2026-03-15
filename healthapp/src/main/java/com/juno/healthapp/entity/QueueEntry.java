package com.juno.healthapp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "queue_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id", nullable = false, unique = true)
    private Visit visit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "symptom_severity", length = 20)
    private String symptomSeverity;

    @Column(name = "pain_level")
    private Integer painLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "symptom_categories", columnDefinition = "jsonb")
    private List<String> symptomCategories;

    @Column(name = "symptom_duration", length = 30)
    private String symptomDuration;

    @Column(name = "additional_notes", columnDefinition = "TEXT")
    private String additionalNotes;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "ai_priority_score", nullable = false)
    @Builder.Default
    private Integer aiPriorityScore = 0;

    @Column(name = "priority_tier", nullable = false, length = 20)
    @Builder.Default
    private String priorityTier = "NON_URGENT";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "score_breakdown", columnDefinition = "jsonb")
    private Object scoreBreakdown;

    @Column(name = "manually_overridden", nullable = false)
    @Builder.Default
    private Boolean manuallyOverridden = false;

    @Column(name = "last_scored_at")
    private LocalDateTime lastScoredAt;

    @Column(name = "last_moved_up_at")
    private LocalDateTime lastMovedUpAt;

    @Column(name = "last_position_updated_at")
    private LocalDateTime lastPositionUpdatedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}