package com.juno.healthapp.service;

import com.juno.healthapp.dto.ScoreResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PriorityScoreService {

    private static final int SEVERITY_WEIGHT  = 40;
    private static final int PAIN_WEIGHT      = 25;
    private static final int CATEGORY_WEIGHT  = 25;
    private static final int DURATION_WEIGHT  = 10;

    private static final Set<String> CRITICAL_CATEGORIES = Set.of(
            "chest", "breathing", "allergy",
            "uncontrolled", "stroke", "confusion",
            "headache", "poisoning", "highbp",
            "oxygen", "extremepain"
    );

    private static final Set<String> HIGH_CATEGORIES = Set.of(
            "heartrate", "highfever", "dizziness",
            "bleeding", "infantfever", "seizure"
    );

    private static final Set<String> MODERATE_CATEGORIES = Set.of(
            "fever", "vomiting", "injury"
    );

    public ScoreResult calculate(
            String symptomSeverity,
            Integer painLevel,
            List<String> symptomCategories,
            String symptomDuration
    ) {
        int severityScore = scoreSeverity(symptomSeverity);
        int painScore     = scorePain(painLevel);
        int categoryScore = scoreCategories(symptomCategories);
        int durationScore = scoreDuration(symptomDuration);

        int total = Math.round(
                (severityScore * SEVERITY_WEIGHT  / 100f) +
                        (painScore     * PAIN_WEIGHT      / 100f) +
                        (categoryScore * CATEGORY_WEIGHT  / 100f) +
                        (durationScore * DURATION_WEIGHT  / 100f)
        );

        if (isCriticalOverride(symptomSeverity, symptomCategories)) {
            total = Math.max(total, 95);
        }

        total = Math.min(100, total);

        Map<String, Object> breakdown = Map.of(
                "severityScore",    severityScore,
                "painScore",        painScore,
                "categoryScore",    categoryScore,
                "durationScore",    durationScore,
                "criticalOverride", isCriticalOverride(symptomSeverity, symptomCategories),
                "finalScore",       total
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
        return Math.min(100, painLevel * 10);
    }

    private int scoreCategories(List<String> categories) {
        if (categories == null || categories.isEmpty()) return 10;

        int score = 10;
        for (String cat : categories) {
            String lower = cat.toLowerCase();
            if (CRITICAL_CATEGORIES.contains(lower)) {
                score += 35;
            } else if (HIGH_CATEGORIES.contains(lower)) {
                score += 22;
            } else if (MODERATE_CATEGORIES.contains(lower)) {
                score += 12;
            } else {
                score += 8;
            }
        }
        return Math.min(100, score);
    }

    private int scoreDuration(String duration) {
        if (duration == null) return 20;
        return switch (duration.toUpperCase()) {
            case "UNDER_1_HOUR"  -> 80;
            case "1_TO_6_HOURS"  -> 60;
            case "6_TO_24_HOURS" -> 40;
            case "OVER_24_HOURS" -> 20;
            default              -> 20;
        };
    }

    private boolean isCriticalOverride(String severity, List<String> categories) {
        if (severity == null) return false;

        boolean isHighSeverity = severity.equalsIgnoreCase("EMERGENCY")
                || severity.equalsIgnoreCase("SEVERE");

        if (!isHighSeverity) return false;
        if (severity.equalsIgnoreCase("EMERGENCY")) return true;

        if (categories == null) return false;
        return categories.stream()
                .anyMatch(c -> CRITICAL_CATEGORIES.contains(c.toLowerCase()));
    }
}