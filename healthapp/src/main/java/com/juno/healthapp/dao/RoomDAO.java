package com.juno.healthapp.dao;

import com.juno.healthapp.entity.Department;
import com.juno.healthapp.entity.Room;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomDAO {
    Room save(Room room);
    Optional<Room> findById(UUID id);
    List<Room> findByDepartment(Department department);
    List<Room> findAvailableByDepartment(Department department);
}