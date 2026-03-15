package com.juno.healthapp.dao;


import com.juno.healthapp.entity.Department;
import com.juno.healthapp.entity.Facility;
import com.juno.healthapp.entity.Patient;
import com.juno.healthapp.entity.Visit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VisitDAO {
    Visit save(Visit visit);
    Optional<Visit> findById(UUID id);
    List<Visit> findByPatient(Patient patient);
    List<Visit> findActiveByDepartment(Department department);
    List<Visit> findActiveByFacility(Facility facility);
    Optional<Visit> findActiveVisitByPatient(Patient patient);
    void delete(Visit visit);
    int countActiveVisitsByFacility(UUID facilityId);
}