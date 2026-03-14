package com.juno.healthapp.dao;

import com.juno.healthapp.entity.AuthAccount;

import java.util.Optional;
import java.util.UUID;

public interface AuthAccountDAO {
    AuthAccount save(AuthAccount authAccount);
    Optional<AuthAccount> findById(UUID id);
    Optional<AuthAccount> findByEmail(String email);
    Optional<AuthAccount> findByPhone(String phone);
    Optional<AuthAccount> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    long countFailedAttemptsByIp(String ipAddress);
    void delete(AuthAccount authAccount);
}