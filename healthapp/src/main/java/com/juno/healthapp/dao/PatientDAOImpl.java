package com.juno.healthapp.dao;

import com.juno.healthapp.entity.Patient;
import com.juno.healthapp.entity.QPatient;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PatientDAOImpl implements PatientDAO {

    @PersistenceContext
    private final EntityManager entityManager;
    private final JPAQueryFactory queryFactory;
    private final QPatient patient = QPatient.patient;

    @Override
    @Transactional
    public Patient save(Patient patient) {
        if (patient.getId() == null) {
            entityManager.persist(patient);
            return patient;
        }
        return entityManager.merge(patient);
    }

    @Override
    public Optional<Patient> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(Patient.class, id));
    }

    @Override
    public Optional<Patient> findByQrToken(String qrToken) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(patient)
                        .where(patient.qrToken.eq(qrToken))
                        .fetchOne()
        );
    }

    @Override
    public Optional<Patient> findByNhfNumber(String nhfNumber) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(patient)
                        .where(patient.nhfNumber.eq(nhfNumber))
                        .fetchOne()
        );
    }

    @Override
    public Optional<Patient> findByNidsNumber(String nidsNumber) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(patient)
                        .where(patient.nidsNumber.eq(nidsNumber))
                        .fetchOne()
        );
    }

    @Override
    public List<Patient> findActivePatients() {
        return queryFactory
                .selectFrom(patient)
                .where(patient.isActive.isTrue())
                .fetch();
    }

    @Override
    public List<Patient> searchByName(String name) {
        return queryFactory
                .selectFrom(patient)
                .where(
                        patient.firstName.containsIgnoreCase(name)
                                .or(patient.lastName.containsIgnoreCase(name))
                                .or(patient.middleName.containsIgnoreCase(name))
                )
                .fetch();
    }

    @Override
    @Transactional
    public void delete(Patient patient) {
        entityManager.remove(entityManager.contains(patient)
                ? patient
                : entityManager.merge(patient));
    }
}