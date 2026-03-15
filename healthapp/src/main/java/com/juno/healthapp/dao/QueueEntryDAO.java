package com.juno.healthapp.dao;


import com.juno.healthapp.entity.Department;
import com.juno.healthapp.entity.Patient;
import com.juno.healthapp.entity.QueueEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QueueEntryDAO {
    QueueEntry save(QueueEntry queueEntry);
    Optional<QueueEntry> findById(UUID id);
    Optional<QueueEntry> findByVisitId(UUID visitId);
    List<QueueEntry> findByDepartmentOrdered(Department department);
    Optional<QueueEntry> findActiveByPatient(Patient patient);
    int countActiveInDepartment(Department department);
    int getNextPosition(Department department);
    void incrementPositionsFrom(Department department, int fromPosition);
    void decrementPositionsAfter(Department department, int position);
    void delete(QueueEntry queueEntry);
}
