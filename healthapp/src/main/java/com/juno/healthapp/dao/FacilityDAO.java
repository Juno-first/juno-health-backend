package com.juno.healthapp.dao;

import com.juno.healthapp.entity.Facility;
import com.juno.healthapp.entity.FacilityService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FacilityDAO {
    Facility save(Facility facility);
    Optional<Facility> findById(UUID id);
    List<Facility> findAll();
    List<Facility> findByParish(String parish);
    Optional<Facility> findByQrToken(String qrToken);
    Optional<Facility> findByCheckinCode(String checkinCode);
    List<FacilityService> findActiveByFacility(Facility facility);
    void delete(Facility facility);
}

