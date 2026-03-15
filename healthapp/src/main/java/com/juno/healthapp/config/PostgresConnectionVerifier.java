package com.juno.healthapp.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PostgresConnectionVerifier {

    private final JdbcTemplate postgresJdbcTemplate;

    public PostgresConnectionVerifier(
            @Qualifier("postgresJdbcTemplate") JdbcTemplate postgresJdbcTemplate) {
        this.postgresJdbcTemplate = postgresJdbcTemplate;
    }

    @PostConstruct
    public void verify() {
        try {
            String version = postgresJdbcTemplate.queryForObject("SELECT version()", String.class);
            System.out.println("✅ Postgres connected! Version: " + version);
        } catch (Exception e) {
            System.err.println("❌ Postgres connection FAILED: " + e.getMessage());
        }
    }
}