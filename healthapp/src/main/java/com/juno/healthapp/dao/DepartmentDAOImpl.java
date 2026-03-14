package com.juno.healthapp.dao;

import com.juno.healthapp.entity.Department;
import com.juno.healthapp.entity.Facility;
import com.juno.healthapp.entity.QDepartment;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DepartmentDAOImpl implements DepartmentDAO {

    @PersistenceContext
    private EntityManager entityManager;

    private final JPAQueryFactory queryFactory;
    private final QDepartment department = QDepartment.department;

    public DepartmentDAOImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    @Transactional
    public Department save(Department department) {
        if (department.getId() == null) {
            entityManager.persist(department);
            return department;
        }
        return entityManager.merge(department);
    }

    @Override
    public Optional<Department> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(Department.class, id));
    }

    @Override
    public List<Department> findByFacility(Facility facility) {
        return queryFactory
                .selectFrom(department)
                .where(department.facility.eq(facility))
                .fetch();
    }

    @Override
    public List<Department> findActiveByFacility(Facility facility) {
        return queryFactory
                .selectFrom(department)
                .where(
                        department.facility.eq(facility),
                        department.isActive.isTrue()
                )
                .fetch();
    }

    @Override
    public Optional<Department> findByFacilityAndType(Facility facility, String departmentType) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(department)
                        .where(
                                department.facility.eq(facility),
                                department.departmentType.eq(departmentType),
                                department.isActive.isTrue()
                        )
                        .fetchOne()
        );
    }

    @Override
    public Optional<Department> findByQrToken(String qrToken) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(department)
                        .where(department.qrToken.eq(qrToken))
                        .fetchOne()
        );
    }

    @Override
    public Optional<Department> findByCheckinCode(String checkinCode) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(department)
                        .where(department.checkinCode.eq(checkinCode))
                        .fetchOne()
        );
    }

    @Override
    @Transactional
    public void delete(Department department) {
        entityManager.remove(entityManager.contains(department)
                ? department
                : entityManager.merge(department));
    }
}
