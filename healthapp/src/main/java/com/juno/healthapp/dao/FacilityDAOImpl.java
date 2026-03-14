package com.juno.healthapp.dao;


import com.juno.healthapp.entity.Facility;
import com.juno.healthapp.entity.FacilityService;
import com.juno.healthapp.entity.QFacility;
import com.juno.healthapp.entity.QFacilityService;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class FacilityDAOImpl implements FacilityDAO {

    @PersistenceContext
    private EntityManager entityManager;

    private final JPAQueryFactory queryFactory;
    private final QFacility facility = QFacility.facility;
    private final QFacilityService facilityService = QFacilityService.facilityService;

    public FacilityDAOImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    @Transactional
    public Facility save(Facility facility) {
        if (facility.getId() == null) {
            entityManager.persist(facility);
            return facility;
        }
        return entityManager.merge(facility);
    }

    @Override
    public Optional<Facility> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(Facility.class, id));
    }

    @Override
    public List<Facility> findAll() {
        return queryFactory
                .selectFrom(facility)
                .orderBy(facility.name.asc())
                .fetch();
    }

    @Override
    public List<Facility> findByParish(String parish) {
        return queryFactory
                .selectFrom(facility)
                .where(facility.parish.equalsIgnoreCase(parish))
                .orderBy(facility.name.asc())
                .fetch();
    }

    @Override
    public Optional<Facility> findByQrToken(String qrToken) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(facility)
                        .where(facility.qrToken.eq(qrToken))
                        .fetchOne()
        );
    }

    @Override
    public Optional<Facility> findByCheckinCode(String checkinCode) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(facility)
                        .where(facility.checkinCode.eq(checkinCode))
                        .fetchOne()
        );
    }

    @Override
    public List<FacilityService> findActiveByFacility(Facility facility) {
        return queryFactory
                .selectFrom(facilityService)
                .where(facilityService.facility.eq(facility)
                        .and(facilityService.isActive.isTrue()))
                .fetch();
    }

    @Override
    @Transactional
    public void delete(Facility facility) {
        entityManager.remove(entityManager.contains(facility)
                ? facility
                : entityManager.merge(facility));
    }
}