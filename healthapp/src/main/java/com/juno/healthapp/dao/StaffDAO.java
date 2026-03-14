package com.juno.healthapp.dao;

import com.juno.healthapp.entity.Department;
import com.juno.healthapp.entity.Staff;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffDAO {
    Staff save(Staff staff);
    Optional<Staff> findById(UUID id);
    Optional<Staff> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Staff> findByRole(String role);
    List<Staff> findByDepartment(Department department);
}
