package com.juno.healthapp.dao;

import com.juno.healthapp.entity.Facility;
import com.juno.healthapp.entity.FacilityService;
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
public class FacilityServiceDAOImpl implements FacilityServiceDAO {

    @PersistenceContext
    private EntityManager entityManager;

    private final JPAQueryFactory queryFactory;
    private final QFacilityService facilityService = QFacilityService.facilityService;

    public FacilityServiceDAOImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    @Transactional
    public FacilityService save(FacilityService service) {
        if (service.getId() == null) {
            entityManager.persist(service);
            return service;
        }
        return entityManager.merge(service);
    }

    @Override
    public Optional<FacilityService> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(FacilityService.class, id));
    }

    @Override
    public List<FacilityService> findByFacility(Facility facility) {
        return queryFactory
                .selectFrom(facilityService)
                .where(facilityService.facility.eq(facility)
                        .and(facilityService.isActive.isTrue()))
                .fetch();
    }

    @Override
    public List<FacilityService> findByFacilityId(UUID facilityId) {
        return queryFactory
                .selectFrom(facilityService)
                .where(facilityService.facility.id.eq(facilityId)
                        .and(facilityService.isActive.isTrue()))
                .fetch();
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        FacilityService service = entityManager.find(FacilityService.class, id);
        if (service != null) {
            service.setIsActive(false);
            entityManager.merge(service);
        }
    }
}
