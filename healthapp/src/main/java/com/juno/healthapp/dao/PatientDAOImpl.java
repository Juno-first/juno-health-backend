package com.juno.healthapp.dao;

import com.juno.healthapp.entity.Patient;
import com.juno.healthapp.entity.QPatient;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


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


}
