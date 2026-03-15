package com.juno.healthapp.controller;

import com.juno.healthapp.dto.OnboardingSurveyRequest;
import com.juno.healthapp.dto.OnboardingSurveyResponse;
import com.juno.healthapp.entity.AuthAccount;
import com.juno.healthapp.service.OnboardingSurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingSurveyController {

    private final OnboardingSurveyService onboardingSurveyService;

    /**
     * GET /api/v1/onboarding
     * Returns existing progress so the frontend can resume from where the patient left off.
     * Returns 204 if no survey started yet.
     */
    @GetMapping
    public ResponseEntity<OnboardingSurveyResponse> getProgress(
            @AuthenticationPrincipal AuthAccount account) {
        OnboardingSurveyResponse response = onboardingSurveyService
                .getProgress(account.getPatient().getId());
        return response != null
                ? ResponseEntity.ok(response)
                : ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/onboarding
     * Save progress for any step(s). Only non-null fields are written.
     * Call this after each step, or at the end with markCompleted=true.
     */
    @PostMapping
    public ResponseEntity<OnboardingSurveyResponse> saveProgress(
            @AuthenticationPrincipal AuthAccount account,
            @RequestBody OnboardingSurveyRequest request) {
        return ResponseEntity.ok(onboardingSurveyService
                .saveProgress(account.getPatient().getId(), request));
    }
}