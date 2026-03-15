package com.juno.healthapp.dao;

import com.juno.healthapp.entity.Patient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientDAO {
    Patient save(Patient patient);
    Optional<Patient> findById(UUID id);
    Optional<Patient> findByQrToken(String qrToken);
    Optional<Patient> findByNhfNumber(String nhfNumber);
    Optional<Patient> findByNidsNumber(String nidsNumber);
    List<Patient> findActivePatients();
    List<Patient> searchByName(String name);
    void delete(Patient patient);
}
