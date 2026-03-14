package com.juno.healthapp.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {}
