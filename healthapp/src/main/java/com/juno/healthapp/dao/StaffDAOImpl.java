package com.juno.healthapp.dao;


import com.juno.healthapp.entity.Department;
import com.juno.healthapp.entity.QStaff;
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
public class StaffDAOImpl implements StaffDAO {

    @PersistenceContext
    private EntityManager entityManager;

    private final JPAQueryFactory queryFactory;
    private final QStaff staff = QStaff.staff;

    public StaffDAOImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    @Transactional
    public Staff save(Staff staff) {
        if (staff.getId() == null) {
            entityManager.persist(staff);
            return staff;
        }
        return entityManager.merge(staff);
    }

    @Override
    public Optional<Staff> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(Staff.class, id));
    }

    @Override
    public Optional<Staff> findByEmail(String email) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(staff)
                        .where(staff.email.eq(email))
                        .fetchOne()
        );
    }

    @Override
    public boolean existsByEmail(String email) {
        return queryFactory
                .selectOne()
                .from(staff)
                .where(staff.email.eq(email))
                .fetchFirst() != null;
    }

    @Override
    public List<Staff> findByRole(String role) {
        return queryFactory
                .selectFrom(staff)
                .where(staff.role.eq(role))
                .fetch();
    }

    @Override
    public List<Staff> findByDepartment(Department department) {
        return queryFactory.selectFrom(staff)
                .where(staff.department.eq(department)
                        .and(staff.isActive.isTrue()))
                .fetch();
    }
}
