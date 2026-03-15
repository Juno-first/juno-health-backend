package com.juno.healthapp.dao;

import com.juno.healthapp.entity.QQueueOverride;
import com.juno.healthapp.entity.QueueEntry;
import com.juno.healthapp.entity.QueueOverride;
import com.juno.healthapp.entity.Staff;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class QueueOverrideDAOImpl implements QueueOverrideDAO {

    @PersistenceContext
    private EntityManager entityManager;

    private final JPAQueryFactory queryFactory;
    private final QQueueOverride queueOverride = QQueueOverride.queueOverride;

    public QueueOverrideDAOImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    @Transactional
    public QueueOverride save(QueueOverride queueOverride) {
        if (queueOverride.getId() == null) {
            entityManager.persist(queueOverride);
            return queueOverride;
        }
        return entityManager.merge(queueOverride);
    }

    @Override
    public Optional<QueueOverride> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(QueueOverride.class, id));
    }

    @Override
    public List<QueueOverride> findByQueueEntry(QueueEntry queueEntry) {
        return queryFactory
                .selectFrom(queueOverride)
                .where(queueOverride.queueEntry.eq(queueEntry))
                .orderBy(queueOverride.overriddenAt.desc())
                .fetch();
    }

    @Override
    public List<QueueOverride> findByStaff(Staff staff) {
        return queryFactory
                .selectFrom(queueOverride)
                .where(queueOverride.staff.eq(staff))
                .orderBy(queueOverride.overriddenAt.desc())
                .fetch();
    }
}