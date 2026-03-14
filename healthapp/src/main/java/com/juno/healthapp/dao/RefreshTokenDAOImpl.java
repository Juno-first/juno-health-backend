package com.juno.healthapp.dao;

import com.juno.healthapp.entity.AuthAccount;
import com.juno.healthapp.entity.QRefreshToken;
import com.juno.healthapp.entity.RefreshToken;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RefreshTokenDAOImpl implements RefreshTokenDAO {

    @PersistenceContext
    private final EntityManager entityManager;
    private final JPAQueryFactory queryFactory;
    private final QRefreshToken refreshToken = QRefreshToken.refreshToken;

    @Override
    @Transactional
    public RefreshToken save(RefreshToken refreshToken) {
        if (refreshToken.getId() == null) {
            entityManager.persist(refreshToken);
            return refreshToken;
        }
        return entityManager.merge(refreshToken);
    }

    @Override
    public Optional<RefreshToken> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(RefreshToken.class, id));
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(refreshToken)
                        .where(
                                refreshToken.tokenHash.eq(tokenHash),
                                refreshToken.isRevoked.isFalse(),
                                refreshToken.expiresAt.after(LocalDateTime.now())
                        )
                        .fetchOne()
        );
    }

    @Override
    public List<RefreshToken> findActiveTokensByAccount(AuthAccount authAccount) {
        return queryFactory
                .selectFrom(refreshToken)
                .where(
                        refreshToken.authAccount.eq(authAccount),
                        refreshToken.isRevoked.isFalse(),
                        refreshToken.expiresAt.after(LocalDateTime.now())
                )
                .fetch();
    }

    @Override
    @Transactional
    public long deleteExpiredTokens() {
        return queryFactory
                .delete(refreshToken)
                .where(refreshToken.expiresAt.before(LocalDateTime.now()))
                .execute();
    }

    @Override
    @Transactional
    public long revokeAllByAccount(AuthAccount authAccount) {
        return queryFactory
                .update(refreshToken)
                .set(refreshToken.isRevoked, true)
                .set(refreshToken.revokedAt, LocalDateTime.now())
                .where(
                        refreshToken.authAccount.eq(authAccount),
                        refreshToken.isRevoked.isFalse()
                )
                .execute();
    }
}
