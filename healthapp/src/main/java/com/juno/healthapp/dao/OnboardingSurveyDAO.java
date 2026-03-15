package com.juno.healthapp.dao;


import com.juno.healthapp.entity.OnboardingSurvey;
import com.juno.healthapp.entity.Patient;

import java.util.Optional;

public interface OnboardingSurveyDAO {
    OnboardingSurvey save(OnboardingSurvey survey);
    Optional<OnboardingSurvey> findByPatient(Patient patient);
}