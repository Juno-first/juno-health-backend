package com.juno.healthapp.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }


    // For STAFF accounts — includes their role (ADMIN, NURSE, DOCTOR etc.)
    public String generateAccessToken(UUID accountId, String email, String accountType,
                                      String staffRole, UUID patientId, UUID staffId,
                                      String firstName, String lastName) {
        var builder = Jwts.builder()
                .subject(accountId.toString())
                .claim("email", email)
                .claim("accountType", accountType)
                .claim("firstName", firstName)
                .claim("lastName", lastName)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey());

        if (staffRole != null) builder.claim("staffRole", staffRole);
        if (patientId != null) builder.claim("patientId", patientId.toString());
        if (staffId != null) builder.claim("staffId", staffId.toString());

        return builder.compact();
    }

    public String extractFirstName(String token) {
        return extractClaims(token).get("firstName", String.class);
    }

    public String extractLastName(String token) {
        return extractClaims(token).get("lastName", String.class);
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public UUID extractPatientId(String token) {
        String value = extractClaims(token).get("patientId", String.class);
        return value != null ? UUID.fromString(value) : null;
    }

    public UUID extractStaffId(String token) {
        String value = extractClaims(token).get("staffId", String.class);
        return value != null ? UUID.fromString(value) : null;
    }


}
