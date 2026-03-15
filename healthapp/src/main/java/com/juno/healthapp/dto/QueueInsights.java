package com.juno.healthapp.dto;

public record QueueInsights(
    long totalPatients,
    long highPriorityPatients,
    double averageWaitTime,
    long patientsProcessed
) {
    public QueueInsights {
        if (totalPatients < 0) totalPatients = 0;
        if (highPriorityPatients < 0) highPriorityPatients = 0;
        if (averageWaitTime < 0) averageWaitTime = 0.0;
        if (patientsProcessed < 0) patientsProcessed = 0;
    }
}
