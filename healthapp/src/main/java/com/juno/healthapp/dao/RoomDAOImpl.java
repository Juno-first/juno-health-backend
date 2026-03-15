package com.juno.healthapp.dao;

import com.juno.healthapp.entity.Department;
import com.juno.healthapp.entity.QRoom;
import com.juno.healthapp.entity.Room;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RoomDAOImpl implements RoomDAO {

    private final QRoom qRoom = QRoom.room;

    @PersistenceContext
    private EntityManager entityManager;

    private final JPAQueryFactory queryFactory;

    public RoomDAOImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Room save(Room room) {
        return entityManager.merge(room);
    }

    @Override
    public Optional<Room> findById(UUID id) {
        return Optional.ofNullable(queryFactory.selectFrom(qRoom)
                .join(qRoom.assignedStaff).fetchJoin()
                .where(qRoom.id.eq(id))
                .fetchOne());
    }

    @Override
    public List<Room> findByDepartment(Department department) {
        return queryFactory.selectFrom(qRoom)
                .leftJoin(qRoom.assignedStaff).fetchJoin()
                .where(qRoom.department.eq(department)
                        .and(qRoom.isActive.isTrue()))
                .fetch();
    }

    @Override
    public List<Room> findAvailableByDepartment(Department department) {
        return queryFactory.selectFrom(qRoom)
                .leftJoin(qRoom.assignedStaff).fetchJoin()
                .where(qRoom.department.eq(department)
                        .and(qRoom.isActive.isTrue())
                        .and(qRoom.isAvailable.isTrue()))
                .fetch();
    }
}
