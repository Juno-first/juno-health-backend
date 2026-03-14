package com.juno.healthapp.dao;

import com.juno.healthapp.entity.AuthAccount;
import com.juno.healthapp.entity.QAuthAccount;
import com.juno.healthapp.entity.QLoginAttempt;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class AuthAccountDAOImpl implements AuthAccountDAO {

    @PersistenceContext
    private EntityManager entityManager;
    private final JPAQueryFactory queryFactory;
    private final QAuthAccount authAccount = QAuthAccount.authAccount;
    private final QLoginAttempt loginAttempt = QLoginAttempt.loginAttempt;

    public AuthAccountDAOImpl(JPAQueryFactory queryFactory){
        this.queryFactory = queryFactory;
    }

    @Override
    @Transactional
    public AuthAccount save(AuthAccount authAccount) {
        if (authAccount.getId() == null) {
            entityManager.persist(authAccount);
            return authAccount;
        }
        return entityManager.merge(authAccount);
    }

    @Override
    public Optional<AuthAccount> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(AuthAccount.class, id));
    }

    @Override
    public Optional<AuthAccount> findByEmail(String email) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(authAccount)
                        .where(authAccount.email.eq(email))
                        .fetchOne()
        );
    }

    @Override
    public Optional<AuthAccount> findByPhone(String phone) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(authAccount)
                        .where(authAccount.phone.eq(phone))
                        .fetchOne()
        );
    }

    @Override
    public Optional<AuthAccount> findByUsername(String username) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(authAccount)
                        .where(authAccount.username.eq(username))
                        .fetchOne()
        );
    }

    @Override
    public boolean existsByEmail(String email) {
        return queryFactory
                .selectOne()
                .from(authAccount)
                .where(authAccount.email.eq(email))
                .fetchFirst() != null;
    }

    @Override
    public boolean existsByPhone(String phone) {
        return queryFactory
                .selectOne()
                .from(authAccount)
                .where(authAccount.phone.eq(phone))
                .fetchFirst() != null;
    }

    @Override
    public long countFailedAttemptsByIp(String ipAddress) {
        return queryFactory
                .selectFrom(loginAttempt)
                .where(
                        loginAttempt.ipAddress.eq(ipAddress),
                        loginAttempt.success.isFalse()
                )
                .fetchCount();
    }

    @Override
    @Transactional
    public void delete(AuthAccount authAccount) {
        entityManager.remove(entityManager.contains(authAccount)
                ? authAccount
                : entityManager.merge(authAccount));
    }
}
