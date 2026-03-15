package com.juno.healthapp.dao;

import com.juno.healthapp.entity.Facility;
import com.juno.healthapp.entity.FacilityService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FacilityServiceDAO {
    FacilityService save(FacilityService service);
    Optional<FacilityService> findById(UUID id);
    List<FacilityService> findByFacility(Facility facility);
    List<FacilityService> findByFacilityId(UUID facilityId);
    void delete(UUID id);
}