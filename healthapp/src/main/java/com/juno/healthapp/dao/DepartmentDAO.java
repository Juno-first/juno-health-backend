package com.juno.healthapp.dao;


import com.juno.healthapp.entity.Department;
import com.juno.healthapp.entity.Facility;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentDAO {
    Department save(Department department);
    Optional<Department> findById(UUID id);
    List<Department> findByFacility(Facility facility);
    List<Department> findActiveByFacility(Facility facility);
    Optional<Department> findByFacilityAndType(Facility facility, String departmentType);
    Optional<Department> findByQrToken(String qrToken);
    Optional<Department> findByCheckinCode(String checkinCode);
    void delete(Department department);
}