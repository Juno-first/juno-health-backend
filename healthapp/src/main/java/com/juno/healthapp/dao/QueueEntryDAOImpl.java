package com.juno.healthapp.dao;


import com.juno.healthapp.entity.Department;
import com.juno.healthapp.entity.Patient;
import com.juno.healthapp.entity.QQueueEntry;
import com.juno.healthapp.entity.QueueEntry;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class QueueEntryDAOImpl implements QueueEntryDAO {

    @PersistenceContext
    private EntityManager entityManager;

    private final JPAQueryFactory queryFactory;
    private final QQueueEntry queueEntry = QQueueEntry.queueEntry;

    public QueueEntryDAOImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    @Transactional
    public QueueEntry save(QueueEntry queueEntry) {
        if (queueEntry.getId() == null) {
            entityManager.persist(queueEntry);
            return queueEntry;
        }
        return entityManager.merge(queueEntry);
    }

    @Override
    public Optional<QueueEntry> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(QueueEntry.class, id));
    }

    @Override
    public Optional<QueueEntry> findByVisitId(UUID visitId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(queueEntry)
                        .where(queueEntry.visit.id.eq(visitId))
                        .fetchOne()
        );
    }

    @Override
    public List<QueueEntry> findByDepartmentOrdered(Department department) {
        return queryFactory
                .selectFrom(queueEntry)
                .where(
                        queueEntry.department.eq(department),
                        queueEntry.visit.status.in("CHECKED_IN", "CALLED")
                )
                .orderBy(queueEntry.position.asc())
                .fetch();
    }

    @Override
    public Optional<QueueEntry> findActiveByPatient(Patient patient) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(queueEntry)
                        .join(queueEntry.patient).fetchJoin()
                        .join(queueEntry.visit).fetchJoin()
                        .where(
                                queueEntry.patient.eq(patient),
                                queueEntry.visit.status.notIn("DISCHARGED", "CANCELLED", "LEFT_QUEUE")
                        )
                        .fetchFirst()
        );
    }

    @Override
    public int countActiveInDepartment(Department department) {
        Long count = queryFactory
                .select(queueEntry.count())
                .from(queueEntry)
                .where(queueEntry.department.eq(department)
                        .and(queueEntry.visit.status.in("CHECKED_IN", "CALLED")))
                .fetchOne();
        return count != null ? count.intValue() : 0;
    }

    @Override
    public int getNextPosition(Department department) {
        Integer maxPosition = queryFactory
                .select(queueEntry.position.max())
                .from(queueEntry)
                .where(
                        queueEntry.department.eq(department),
                        queueEntry.visit.status.in("CHECKED_IN", "CALLED")
                )
                .fetchOne();

        return maxPosition == null ? 1 : maxPosition + 1;
    }

    // DAOImpl
    @Override
    public void incrementPositionsFrom(Department department, int fromPosition) {
        queryFactory.update(queueEntry)
                .set(queueEntry.position, queueEntry.position.add(1))
                .where(queueEntry.department.eq(department)
                        .and(queueEntry.position.goe(fromPosition))
                        .and(queueEntry.visit.status.in("CHECKED_IN", "CALLED")))
                .execute();
    }

    @Override
    @Transactional
    public void decrementPositionsAfter(Department department, int position) {
        queryFactory
                .update(queueEntry)
                .set(queueEntry.position, queueEntry.position.subtract(1))
                .where(queueEntry.department.eq(department)
                        .and(queueEntry.position.gt(position))
                        .and(queueEntry.visit.status.in("CHECKED_IN", "CALLED")))
                .execute();
    }

    @Override
    @Transactional
    public void delete(QueueEntry queueEntry) {
        entityManager.remove(entityManager.contains(queueEntry)
                ? queueEntry
                : entityManager.merge(queueEntry));
    }
}
