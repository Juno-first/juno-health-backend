package com.juno.healthapp.service;

import com.juno.healthapp.dto.ScoreResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PriorityScoreService {

    // ── Weights (must sum to 100) ──────────────────────────────────────────
    private static final int SEVERITY_WEIGHT  = 40;
    private static final int PAIN_WEIGHT      = 25;
    private static final int CATEGORY_WEIGHT  = 25;
    private static final int DURATION_WEIGHT  = 10;

    // ── High-risk categories that boost score ──────────────────────────────
    private static final Set<String> CRITICAL_CATEGORIES = Set.of(
            "CHEST_PAIN", "DIFFICULTY_BREATHING", "ALLERGIC_REACTION"
    );
    private static final Set<String> HIGH_CATEGORIES = Set.of(
            "BLEEDING", "DIZZINESS"
    );

    public ScoreResult calculate(
            String symptomSeverity,
            Integer painLevel,
            List<String> symptomCategories,
            String symptomDuration
    ) {
        int severityScore  = scoreSeverity(symptomSeverity);
        int painScore      = scorePain(painLevel);
        int categoryScore  = scoreCategories(symptomCategories);
        int durationScore  = scoreDuration(symptomDuration);

        // Weighted total (0–100)
        int total = Math.round(
                (severityScore  * SEVERITY_WEIGHT  / 100f) +
                        (painScore      * PAIN_WEIGHT      / 100f) +
                        (categoryScore  * CATEGORY_WEIGHT  / 100f) +
                        (durationScore  * DURATION_WEIGHT  / 100f)
        );

        // Hard override — certain category combos are always critical
        if (isCriticalOverride(symptomSeverity, symptomCategories)) {
            total = Math.max(total, 95);
        }

        total = Math.min(100, total);

        Map<String, Object> breakdown = Map.of(
                "severityScore",      severityScore,
                "painScore",          painScore,
                "categoryScore",      categoryScore,
                "durationScore",      durationScore,
                "criticalOverride",   isCriticalOverride(symptomSeverity, symptomCategories),
                "finalScore",         total
        );

        return new ScoreResult(total, breakdown);
    }

    private int scoreSeverity(String severity) {
        if (severity == null) return 30;
        return switch (severity.toUpperCase()) {
            case "EMERGENCY" -> 100;
            case "SEVERE"    -> 75;
            case "MODERATE"  -> 45;
            case "MILD"      -> 15;
            default          -> 30;
        };
    }

    private int scorePain(Integer painLevel) {
        if (painLevel == null) return 20;
        // Pain 0–10 mapped to 0–100
        return Math.min(100, painLevel * 10);
    }

    private int scoreCategories(List<String> categories) {
        if (categories == null || categories.isEmpty()) return 10;

        int score = 10;
        for (String cat : categories) {
            String upper = cat.toUpperCase();
            if (CRITICAL_CATEGORIES.contains(upper)) {
                score += 30;
            } else if (HIGH_CATEGORIES.contains(upper)) {
                score += 20;
            } else {
                // FEVER, VOMITING, INJURY
                score += 10;
            }
        }
        return Math.min(100, score);
    }

    private int scoreDuration(String duration) {
        if (duration == null) return 20;
        return switch (duration.toUpperCase()) {
            // Short duration + severe symptoms = more urgent (acute onset)
            case "UNDER_1_HOUR"   -> 80;
            case "1_TO_6_HOURS"   -> 60;
            case "6_TO_24_HOURS"  -> 40;
            case "OVER_24_HOURS"  -> 20;
            default               -> 20;
        };
    }

    // Emergency severity + any critical category always floors at 90
    private boolean isCriticalOverride(String severity, List<String> categories) {
        if ("EMERGENCY".equalsIgnoreCase(severity)) return true;
        if (categories == null) return false;
        return categories.stream()
                .anyMatch(c -> CRITICAL_CATEGORIES.contains(c.toUpperCase()));
    }
}