package com.juno.healthapp.service;


import com.juno.healthapp.dao.*;
import com.juno.healthapp.dto.*;
import com.juno.healthapp.entity.*;
import com.juno.healthapp.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthAccountDAO authAccountDAO;
    private final RefreshTokenDAO refreshTokenDAO;
    private final PatientDAO patientDAO;
    private final PasswordEncoder passwordEncoder;
    private final StaffDAO staffDAO;
    private final FacilityDAO facilityDAO;
    private final DepartmentDAO departmentDAO;

    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (authAccountDAO.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use");
        }
        if (authAccountDAO.existsByPhone(request.phone())) {
            throw new IllegalArgumentException("Phone number already in use");
        }

        Patient patient = Patient.builder()
                .firstName(request.firstName())
                .middleName(request.middleName())
                .lastName(request.lastName())
                .dateOfBirth(request.dateOfBirth())
                .gender(request.gender())
                .parishOfResidence(request.parishOfResidence())
                .languagePreference(request.languagePreference())
                .build();

        patient = patientDAO.save(patient);

        AuthAccount account = AuthAccount.builder()
                .accountType("PATIENT")
                .patient(patient)
                .email(request.email())
                .phone(request.phone())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();
        account = authAccountDAO.save(account);

        return generateAuthResponse(account);
    }

    @Transactional
    public AuthResponse registerStaff(StaffRegisterRequest request) {
        if (authAccountDAO.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use");
        }

        // Resolve facility if provided
        Facility facility = null;
        if (request.facilityId() != null) {
            facility = facilityDAO.findById(request.facilityId())
                    .orElseThrow(() -> new IllegalArgumentException("Facility not found"));
        }

        // Create staff record
        Staff staff = Staff.builder()
                .facility(facility)
                .department(request.departmentId() != null ? departmentDAO.findById(request.departmentId())
                                                             .orElseThrow(() -> new IllegalArgumentException("Department not found")) : null)
                .firstName(request.firstName())
                .middleName(request.middleName())
                .lastName(request.lastName())
                .email(request.email())
                .role(request.role())
                .specialty(request.specialty())
                .isActive(true)
                .isOnDuty(false)
                .createdAt(LocalDateTime.now())
                .build();

        staff = staffDAO.save(staff);

        // Create auth account
        AuthAccount account = AuthAccount.builder()
                .accountType("STAFF")
                .staff(staff)
                .email(request.email())
                .phone(request.phone())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();
        account = authAccountDAO.save(account);

        return generateAuthResponse(account);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        AuthAccount account = authAccountDAO.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (account.getIsLocked() && account.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Account is locked. Try again later.");
        }

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            handleFailedAttempt(account);
            throw new IllegalArgumentException("Invalid credentials");
        }

        account.setFailedAttempts(0);
        account.setIsLocked(false);
        account.setLastLoginAt(LocalDateTime.now());
        authAccountDAO.save(account);

        return generateAuthResponse(account);
    }

    private AuthResponse generateAuthResponse(AuthAccount account) {
        String staffRole = null;
        UUID patientId = null;
        UUID staffId = null;
        String firstName = null;
        String lastName = null;

        if ("STAFF".equals(account.getAccountType()) && account.getStaff() != null) {
            staffRole = account.getStaff().getRole();
            staffId = account.getStaff().getId();
            firstName = account.getStaff().getFirstName();
            lastName = account.getStaff().getLastName();
        }

        if ("PATIENT".equals(account.getAccountType()) && account.getPatient() != null) {
            patientId = account.getPatient().getId();
            firstName = account.getPatient().getFirstName();
            lastName = account.getPatient().getLastName();
        }

        String accessToken = jwtUtil.generateAccessToken(
                account.getId(),
                account.getEmail(),
                account.getAccountType(),
                staffRole,
                patientId,
                staffId,
                firstName,
                lastName
        );

        String rawRefreshToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .authAccount(account)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
        refreshTokenDAO.save(refreshToken);

        return new AuthResponse(accessToken, rawRefreshToken);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.refreshToken());

        RefreshToken refreshToken = refreshTokenDAO.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired refresh token"));

        AuthAccount account = refreshToken.getAuthAccount();

        if (account.getIsLocked()) {
            throw new IllegalStateException("Account is locked");
        }

        // Revoke old refresh token
        refreshToken.setIsRevoked(true);
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenDAO.save(refreshToken);

        // Generate new tokens
        return generateAuthResponse(account);
    }

    private void handleFailedAttempt(AuthAccount account) {
        int attempts = account.getFailedAttempts() + 1;
        account.setFailedAttempts(attempts);
        if (attempts >= 5) {
            account.setIsLocked(true);
            account.setLockedUntil(LocalDateTime.now().plusMinutes(15));
        }
        authAccountDAO.save(account);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

}