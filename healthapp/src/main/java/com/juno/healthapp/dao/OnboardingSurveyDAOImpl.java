package com.juno.healthapp.dao;

import com.juno.healthapp.entity.OnboardingSurvey;
import com.juno.healthapp.entity.Patient;
import com.juno.healthapp.entity.QOnboardingSurvey;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class OnboardingSurveyDAOImpl implements OnboardingSurveyDAO {

    private final QOnboardingSurvey qSurvey = QOnboardingSurvey.onboardingSurvey;

    @PersistenceContext
    private EntityManager entityManager;

    private final JPAQueryFactory queryFactory;

    public OnboardingSurveyDAOImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public OnboardingSurvey save(OnboardingSurvey survey) {
        return entityManager.merge(survey);
    }

    @Override
    public Optional<OnboardingSurvey> findByPatient(Patient patient) {
        return Optional.ofNullable(queryFactory.selectFrom(qSurvey)
                .where(qSurvey.patient.eq(patient))
                .fetchOne());
    }
}