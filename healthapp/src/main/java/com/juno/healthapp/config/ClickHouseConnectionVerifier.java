package com.juno.healthapp.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ClickHouseConnectionVerifier {

    private final JdbcTemplate clickHouseJdbcTemplate;

    public ClickHouseConnectionVerifier(
            @Qualifier("clickHouseJdbcTemplate") JdbcTemplate clickHouseJdbcTemplate) {
        this.clickHouseJdbcTemplate = clickHouseJdbcTemplate;
    }

    @PostConstruct
    public void verify() {
        try {
            // ✅ String, not UUID
            String version = clickHouseJdbcTemplate.queryForObject("SELECT version()", String.class);
            System.out.println("✅ ClickHouse connected! Version: " + version);
        } catch (Exception e) {
            System.err.println("❌ ClickHouse connection FAILED: " + e.getMessage());
        }
    }
}