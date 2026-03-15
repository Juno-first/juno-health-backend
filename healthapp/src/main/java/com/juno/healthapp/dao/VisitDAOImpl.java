package com.juno.healthapp.dao;

import com.juno.healthapp.entity.*;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class VisitDAOImpl implements VisitDAO {

    @PersistenceContext
    private EntityManager entityManager;

    private final JPAQueryFactory queryFactory;
    private final QVisit visit = QVisit.visit;

    public VisitDAOImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    @Transactional
    public Visit save(Visit visit) {
        if (visit.getId() == null) {
            entityManager.persist(visit);
            return visit;
        }
        return entityManager.merge(visit);
    }

    @Override
    public Optional<Visit> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(Visit.class, id));
    }

    @Override
    public List<Visit> findByPatient(Patient patient) {
        return queryFactory
                .selectFrom(visit)
                .where(visit.patient.eq(patient))
                .orderBy(visit.checkedInAt.desc())
                .fetch();
    }

    @Override
    public List<Visit> findActiveByDepartment(Department department) {
        return queryFactory
                .selectFrom(visit)
                .where(
                        visit.department.eq(department),
                        visit.status.notIn("DISCHARGED", "CANCELLED")
                )
                .orderBy(visit.checkedInAt.asc())
                .fetch();
    }

    @Override
    public List<Visit> findActiveByFacility(Facility facility) {
        return queryFactory
                .selectFrom(visit)
                .where(
                        visit.facility.eq(facility),
                        visit.status.notIn("DISCHARGED", "CANCELLED")
                )
                .orderBy(visit.checkedInAt.asc())
                .fetch();
    }

    @Override
    public Optional<Visit> findActiveVisitByPatient(Patient patient) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(visit)
                        .where(visit.patient.eq(patient)
                                .and(visit.status.notIn("DISCHARGED", "LEFT_QUEUE")))
                        .fetchFirst()
        );
    }

    @Override
    @Transactional
    public void delete(Visit visit) {
        entityManager.remove(entityManager.contains(visit)
                ? visit
                : entityManager.merge(visit));
    }

    @Override
    public int countActiveVisitsByFacility(UUID facilityId) {
        Long count = queryFactory
                .select(visit.count())
                .from(visit)
                .where(visit.facility.id.eq(facilityId)
                        .and(visit.status.in("CHECKED_IN", "CALLED")))
                .fetchOne();
        return count != null ? count.intValue() : 0;
    }
}
