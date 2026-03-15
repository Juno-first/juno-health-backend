package com.juno.healthapp.dto;

import java.util.Map;

public record ScoreResult(int score, Map<String, Object> breakdown) {}