package com.juno.healthapp.dto;

public record LoginRequest(
        String email,
        String password
) {}