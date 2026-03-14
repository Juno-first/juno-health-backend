package com.juno.healthapp.dto;

import java.time.LocalDate;

public record RegisterRequest(
        String firstName,
        String middleName,
        String lastName,
        LocalDate dateOfBirth,
        String gender,
        String parishOfResidence,
        String languagePreference,
        String email,
        String phone,
        String password
) {}