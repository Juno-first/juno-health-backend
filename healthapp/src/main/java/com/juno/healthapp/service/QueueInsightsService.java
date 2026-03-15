package com.juno.healthapp.service;

import com.juno.healthapp.dto.QueueInsights;
import com.juno.healthapp.repository.QueueInsightsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueInsightsService {

    private final QueueInsightsRepository queueInsightsRepository;

    public QueueInsights getQueueStatus() {
        try {
            // Option 1: Use individual queries (similar to your Python version)
            // long totalPatients = queueInsightsRepository.getTotalPatients();
            // long highPriorityPatients = queueInsightsRepository.getHighPriorityPatients();
            // double averageWaitTime = queueInsightsRepository.getAverageWaitTime();
            // long patientsProcessed = queueInsightsRepository.getPatientsProcessedToday();

            // return new QueueInsights(
            //     totalPatients,
            //     highPriorityPatients,
            //     averageWaitTime,
            //     patientsProcessed
            // );

            // Option 2: Use single combined query (more efficient)
            Map<String, Object> insights = queueInsightsRepository.getAllInsights();
            return new QueueInsights(
                ((Number) insights.get("total_patients")).longValue(),
                ((Number) insights.get("high_priority_patients")).longValue(),
                ((Number) insights.get("avg_wait_seconds")).doubleValue(),
                ((Number) insights.get("patients_processed")).longValue()
            );

        } catch (Exception e) {
            log.error("Error getting queue insights: {}", e.getMessage(), e);
            // Return empty insights instead of throwing exception
            return new QueueInsights(0L, 0L, 0.0, 0L);
        }
    }

    public Map<String, Object> getDetailedInsights() {
        try {
            return queueInsightsRepository.getAllInsights();
        } catch (Exception e) {
            log.error("Error getting detailed insights: {}", e.getMessage(), e);
            return Map.of(
                "total_patients", 0L,
                "high_priority_patients", 0L,
                "avg_wait_seconds", 0.0,
                "patients_processed", 0L,
                "error", "Failed to fetch insights"
            );
        }
    }

    // Enhanced insights using enriched patient_queue data
    
    public List<Map<String, Object>> getAverageWaitTimeBySymptomSeverity() {
        try {
            return queueInsightsRepository.getAverageWaitTimeBySymptomSeverity();
        } catch (Exception e) {
            log.error("Error getting average wait time by symptom severity: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<Map<String, Object>> getDepartmentPerformanceMetrics() {
        try {
            return queueInsightsRepository.getDepartmentPerformanceMetrics();
        } catch (Exception e) {
            log.error("Error getting department performance metrics: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<Map<String, Object>> getPainLevelAnalysis() {
        try {
            return queueInsightsRepository.getPainLevelAnalysis();
        } catch (Exception e) {
            log.error("Error getting pain level analysis: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<Map<String, Object>> getSymptomCategoryAnalysis() {
        try {
            return queueInsightsRepository.getSymptomCategoryAnalysis();
        } catch (Exception e) {
            log.error("Error getting symptom category analysis: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<Map<String, Object>> getHourlyQueueTrends() {
        try {
            return queueInsightsRepository.getHourlyQueueTrends();
        } catch (Exception e) {
            log.error("Error getting hourly queue trends: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<Map<String, Object>> getStaffAssignmentAnalytics() {
        try {
            return queueInsightsRepository.getStaffAssignmentAnalytics();
        } catch (Exception e) {
            log.error("Error getting staff assignment analytics: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public Map<String, Object> getComprehensiveInsights() {
        try {
            return queueInsightsRepository.getComprehensiveInsights();
        } catch (Exception e) {
            log.error("Error getting comprehensive insights: {}", e.getMessage(), e);
            return new java.util.HashMap<>() {{
                put("total_checkins", 0L);
                put("checkins_today", 0L);
                put("called_today", 0L);
                put("discharged_today", 0L);
                put("avg_wait_time", 0.0);
                put("avg_priority_score", 0.0);
                put("high_priority_patients", 0L);
                put("severe_symptom_patients", 0L);
                put("high_pain_patients", 0L);
                put("multiple_symptom_patients", 0L);
                put("error", "Failed to fetch comprehensive insights");
            }};
        }
    }

    // Historical trend analysis methods
    
    public List<Map<String, Object>> getDailyPatientVolumeTrends(int days) {
        try {
            return queueInsightsRepository.getDailyPatientVolumeTrends(days);
        } catch (Exception e) {
            log.error("Error getting daily patient volume trends: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<Map<String, Object>> getWeeklyDepartmentTrends(int weeks) {
        try {
            return queueInsightsRepository.getWeeklyDepartmentTrends(weeks);
        } catch (Exception e) {
            log.error("Error getting weekly department trends: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<Map<String, Object>> getMonthlySymptomTrends(int months) {
        try {
            return queueInsightsRepository.getMonthlySymptomTrends(months);
        } catch (Exception e) {
            log.error("Error getting monthly symptom trends: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<Map<String, Object>> getHourlyWaitTimeTrends(int days) {
        try {
            return queueInsightsRepository.getHourlyWaitTimeTrends(days);
        } catch (Exception e) {
            log.error("Error getting hourly wait time trends: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<Map<String, Object>> getStaffPerformanceTrends(int days) {
        try {
            return queueInsightsRepository.getStaffPerformanceTrends(days);
        } catch (Exception e) {
            log.error("Error getting staff performance trends: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<Map<String, Object>> getQueueDepthTrends(int days) {
        try {
            return queueInsightsRepository.getQueueDepthTrends(days);
        } catch (Exception e) {
            log.error("Error getting queue depth trends: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<Map<String, Object>> getSymptomCategoryEvolution(int months) {
        try {
            return queueInsightsRepository.getSymptomCategoryEvolution(months);
        } catch (Exception e) {
            log.error("Error getting symptom category evolution: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<Map<String, Object>> getPriorityScoreDistributionTrends(int days) {
        try {
            return queueInsightsRepository.getPriorityScoreDistributionTrends(days);
        } catch (Exception e) {
            log.error("Error getting priority score distribution trends: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public Map<String, Object> getHistoricalComparisons() {
        try {
            return queueInsightsRepository.getHistoricalComparisons();
        } catch (Exception e) {
            log.error("Error getting historical comparisons: {}", e.getMessage(), e);
            return Map.of("error", "Failed to fetch historical comparisons");
        }
    }

    // Dashboard-specific real-time insights methods
    
    public Map<String, Object> getRealtimeDashboardMetrics() {
        try {
            return queueInsightsRepository.getRealtimeDashboardMetrics();
        } catch (Exception e) {
            log.error("Error getting realtime dashboard metrics: {}", e.getMessage(), e);
            return Map.of("error", "Failed to fetch dashboard metrics");
        }
    }

    public List<Map<String, Object>> getDepartmentLoadAnalysis() {
        try {
            return queueInsightsRepository.getDepartmentLoadAnalysis();
        } catch (Exception e) {
            log.error("Error getting department load analysis: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<Map<String, Object>> getStaffUtilizationMetrics() {
        try {
            return queueInsightsRepository.getStaffUtilizationMetrics();
        } catch (Exception e) {
            log.error("Error getting staff utilization metrics: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public Map<String, Object> getWaitTimeAnalysis() {
        try {
            return queueInsightsRepository.getWaitTimeAnalysis();
        } catch (Exception e) {
            log.error("Error getting wait time analysis: {}", e.getMessage(), e);
            return Map.of("error", "Failed to fetch wait time analysis");
        }
    }

    public List<Map<String, Object>> getSymptomTrendAlerts() {
        try {
            return queueInsightsRepository.getSymptomTrendAlerts();
        } catch (Exception e) {
            log.error("Error getting symptom trend alerts: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public Map<String, Object> getCapacityUtilizationMetrics() {
        try {
            return queueInsightsRepository.getCapacityUtilizationMetrics();
        } catch (Exception e) {
            log.error("Error getting capacity utilization metrics: {}", e.getMessage(), e);
            return Map.of("error", "Failed to fetch capacity metrics");
        }
    }

    public List<Map<String, Object>> getHourlyPerformanceBreakdown() {
        try {
            return queueInsightsRepository.getHourlyPerformanceBreakdown();
        } catch (Exception e) {
            log.error("Error getting hourly performance breakdown: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public Map<String, Object> getComprehensiveDashboardOverview() {
        try {
            Map<String, Object> overview = new java.util.HashMap<>();
            
            // Combine all dashboard metrics into one comprehensive view
            overview.put("realtime_metrics", getRealtimeDashboardMetrics());
            overview.put("department_load", getDepartmentLoadAnalysis());
            overview.put("staff_utilization", getStaffUtilizationMetrics());
            overview.put("wait_time_analysis", getWaitTimeAnalysis());
            overview.put("symptom_alerts", getSymptomTrendAlerts());
            overview.put("capacity_metrics", getCapacityUtilizationMetrics());
            overview.put("hourly_performance", getHourlyPerformanceBreakdown());
            
            // Add timestamp for freshness
            overview.put("last_updated", java.time.Instant.now().toString());
            
            return overview;
        } catch (Exception e) {
            log.error("Error getting comprehensive dashboard overview: {}", e.getMessage(), e);
            return Map.of("error", "Failed to fetch comprehensive dashboard overview");
        }
    }
}
