package com.juno.healthapp.dao;

import com.juno.healthapp.entity.QueueEntry;
import com.juno.healthapp.entity.QueueOverride;
import com.juno.healthapp.entity.Staff;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QueueOverrideDAO {
    QueueOverride save(QueueOverride queueOverride);
    Optional<QueueOverride> findById(UUID id);
    List<QueueOverride> findByQueueEntry(QueueEntry queueEntry);
    List<QueueOverride> findByStaff(Staff staff);
}
