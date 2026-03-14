package com.juno.healthapp.dao;


import com.juno.healthapp.entity.AuthAccount;
import com.juno.healthapp.entity.RefreshToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenDAO {
    RefreshToken save(RefreshToken refreshToken);
    Optional<RefreshToken> findById(UUID id);
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    List<RefreshToken> findActiveTokensByAccount(AuthAccount authAccount);
    long deleteExpiredTokens();
    long revokeAllByAccount(AuthAccount authAccount);
}
