package com.juno.healthapp.controller;

import com.juno.healthapp.dto.QueueInsights;
import com.juno.healthapp.service.QueueInsightsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/insights")
@RequiredArgsConstructor
public class QueueInsightsController {

    private final QueueInsightsService queueInsightsService;

    @GetMapping("/queue-status")
    public ResponseEntity<QueueInsights> getQueueStatus() {
        try {
            QueueInsights insights = queueInsightsService.getQueueStatus();
            log.info("Retrieved queue insights: {}", insights);
            return ResponseEntity.ok(insights);
        } catch (Exception e) {
            log.error("Error getting queue status", e);
            // Return empty insights with 200 status (like your Python version doesn't throw)
            return ResponseEntity.ok(new QueueInsights(0L, 0L, 0.0, 0L));
        }
    }

    @GetMapping("/queue-status/detailed")
    public ResponseEntity<Map<String, Object>> getDetailedQueueStatus() {
        try {
            Map<String, Object> insights = queueInsightsService.getDetailedInsights();
            return ResponseEntity.ok(insights);
        } catch (Exception e) {
            log.error("Error getting detailed queue status", e);
            return ResponseEntity.ok(Map.of(
                "total_patients", 0L,
                "high_priority_patients", 0L,
                "avg_wait_seconds", 0.0,
                "patients_processed", 0L,
                "error", "Failed to fetch insights"
            ));
        }
    }

    // Enhanced endpoints for detailed insights using enriched patient_queue data
    
    @GetMapping("/symptom-severity-wait-times")
    public ResponseEntity<List<Map<String, Object>>> getAverageWaitTimeBySymptomSeverity() {
        try {
            List<Map<String, Object>> insights = queueInsightsService.getAverageWaitTimeBySymptomSeverity();
            log.info("Retrieved average wait time by symptom severity: {} records", insights.size());
            return ResponseEntity.ok(insights);
        } catch (Exception e) {
            log.error("Error getting average wait time by symptom severity", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/department-performance")
    public ResponseEntity<List<Map<String, Object>>> getDepartmentPerformanceMetrics() {
        try {
            List<Map<String, Object>> insights = queueInsightsService.getDepartmentPerformanceMetrics();
            log.info("Retrieved department performance metrics: {} records", insights.size());
            return ResponseEntity.ok(insights);
        } catch (Exception e) {
            log.error("Error getting department performance metrics", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/pain-level-analysis")
    public ResponseEntity<List<Map<String, Object>>> getPainLevelAnalysis() {
        try {
            List<Map<String, Object>> insights = queueInsightsService.getPainLevelAnalysis();
            log.info("Retrieved pain level analysis: {} records", insights.size());
            return ResponseEntity.ok(insights);
        } catch (Exception e) {
            log.error("Error getting pain level analysis", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/symptom-category-analysis")
    public ResponseEntity<List<Map<String, Object>>> getSymptomCategoryAnalysis() {
        try {
            List<Map<String, Object>> insights = queueInsightsService.getSymptomCategoryAnalysis();
            log.info("Retrieved symptom category analysis: {} records", insights.size());
            return ResponseEntity.ok(insights);
        } catch (Exception e) {
            log.error("Error getting symptom category analysis", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/hourly-trends")
    public ResponseEntity<List<Map<String, Object>>> getHourlyQueueTrends() {
        try {
            List<Map<String, Object>> insights = queueInsightsService.getHourlyQueueTrends();
            log.info("Retrieved hourly queue trends: {} records", insights.size());
            return ResponseEntity.ok(insights);
        } catch (Exception e) {
            log.error("Error getting hourly queue trends", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/staff-analytics")
    public ResponseEntity<List<Map<String, Object>>> getStaffAssignmentAnalytics() {
        try {
            List<Map<String, Object>> insights = queueInsightsService.getStaffAssignmentAnalytics();
            log.info("Retrieved staff assignment analytics: {} records", insights.size());
            return ResponseEntity.ok(insights);
        } catch (Exception e) {
            log.error("Error getting staff assignment analytics", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/comprehensive")
    public ResponseEntity<Map<String, Object>> getComprehensiveInsights() {
        try {
            Map<String, Object> insights = queueInsightsService.getComprehensiveInsights();
            log.info("Retrieved comprehensive insights");
            return ResponseEntity.ok(insights);
        } catch (Exception e) {
            log.error("Error getting comprehensive insights", e);
            return ResponseEntity.ok(new java.util.HashMap<>() {{
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
            }});
        }
    }

    // Historical trend analysis endpoints
    
    @GetMapping("/trends/daily-volume")
    public ResponseEntity<List<Map<String, Object>>> getDailyPatientVolumeTrends(
            @RequestParam(defaultValue = "30") int days) {
        try {
            List<Map<String, Object>> trends = queueInsightsService.getDailyPatientVolumeTrends(days);
            log.info("Retrieved daily patient volume trends for {} days: {} records", days, trends.size());
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            log.error("Error getting daily patient volume trends", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/trends/weekly-departments")
    public ResponseEntity<List<Map<String, Object>>> getWeeklyDepartmentTrends(
            @RequestParam(defaultValue = "12") int weeks) {
        try {
            List<Map<String, Object>> trends = queueInsightsService.getWeeklyDepartmentTrends(weeks);
            log.info("Retrieved weekly department trends for {} weeks: {} records", weeks, trends.size());
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            log.error("Error getting weekly department trends", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/trends/monthly-symptoms")
    public ResponseEntity<List<Map<String, Object>>> getMonthlySymptomTrends(
            @RequestParam(defaultValue = "12") int months) {
        try {
            List<Map<String, Object>> trends = queueInsightsService.getMonthlySymptomTrends(months);
            log.info("Retrieved monthly symptom trends for {} months: {} records", months, trends.size());
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            log.error("Error getting monthly symptom trends", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/trends/hourly-wait-times")
    public ResponseEntity<List<Map<String, Object>>> getHourlyWaitTimeTrends(
            @RequestParam(defaultValue = "7") int days) {
        try {
            List<Map<String, Object>> trends = queueInsightsService.getHourlyWaitTimeTrends(days);
            log.info("Retrieved hourly wait time trends for {} days: {} records", days, trends.size());
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            log.error("Error getting hourly wait time trends", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/trends/staff-performance")
    public ResponseEntity<List<Map<String, Object>>> getStaffPerformanceTrends(
            @RequestParam(defaultValue = "7") int days) {
        try {
            List<Map<String, Object>> trends = queueInsightsService.getStaffPerformanceTrends(days);
            log.info("Retrieved staff performance trends for {} days: {} records", days, trends.size());
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            log.error("Error getting staff performance trends", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/trends/queue-depth")
    public ResponseEntity<List<Map<String, Object>>> getQueueDepthTrends(
            @RequestParam(defaultValue = "7") int days) {
        try {
            List<Map<String, Object>> trends = queueInsightsService.getQueueDepthTrends(days);
            log.info("Retrieved queue depth trends for {} days: {} records", days, trends.size());
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            log.error("Error getting queue depth trends", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/trends/symptom-evolution")
    public ResponseEntity<List<Map<String, Object>>> getSymptomCategoryEvolution(
            @RequestParam(defaultValue = "12") int months) {
        try {
            List<Map<String, Object>> trends = queueInsightsService.getSymptomCategoryEvolution(months);
            log.info("Retrieved symptom category evolution for {} months: {} records", months, trends.size());
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            log.error("Error getting symptom category evolution", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/trends/priority-distribution")
    public ResponseEntity<List<Map<String, Object>>> getPriorityScoreDistributionTrends(
            @RequestParam(defaultValue = "7") int days) {
        try {
            List<Map<String, Object>> trends = queueInsightsService.getPriorityScoreDistributionTrends(days);
            log.info("Retrieved priority score distribution trends for {} days: {} records", days, trends.size());
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            log.error("Error getting priority score distribution trends", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/trends/historical-comparisons")
    public ResponseEntity<Map<String, Object>> getHistoricalComparisons() {
        try {
            Map<String, Object> comparisons = queueInsightsService.getHistoricalComparisons();
            log.info("Retrieved historical comparisons");
            return ResponseEntity.ok(comparisons);
        } catch (Exception e) {
            log.error("Error getting historical comparisons", e);
            return ResponseEntity.ok(Map.of("error", "Failed to fetch historical comparisons"));
        }
    }

    // Dashboard-specific endpoints for admin views
    
    @GetMapping("/dashboard/realtime")
    public ResponseEntity<Map<String, Object>> getRealtimeDashboardMetrics() {
        try {
            Map<String, Object> metrics = queueInsightsService.getRealtimeDashboardMetrics();
            log.info("Retrieved realtime dashboard metrics");
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error getting realtime dashboard metrics", e);
            return ResponseEntity.ok(Map.of("error", "Failed to fetch dashboard metrics"));
        }
    }

    @GetMapping("/dashboard/department-load")
    public ResponseEntity<List<Map<String, Object>>> getDepartmentLoadAnalysis() {
        try {
            List<Map<String, Object>> analysis = queueInsightsService.getDepartmentLoadAnalysis();
            log.info("Retrieved department load analysis: {} departments", analysis.size());
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            log.error("Error getting department load analysis", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/dashboard/staff-utilization")
    public ResponseEntity<List<Map<String, Object>>> getStaffUtilizationMetrics() {
        try {
            List<Map<String, Object>> metrics = queueInsightsService.getStaffUtilizationMetrics();
            log.info("Retrieved staff utilization metrics: {} staff members", metrics.size());
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error getting staff utilization metrics", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/dashboard/wait-time-analysis")
    public ResponseEntity<Map<String, Object>> getWaitTimeAnalysis() {
        try {
            Map<String, Object> analysis = queueInsightsService.getWaitTimeAnalysis();
            log.info("Retrieved wait time analysis");
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            log.error("Error getting wait time analysis", e);
            return ResponseEntity.ok(Map.of("error", "Failed to fetch wait time analysis"));
        }
    }

    @GetMapping("/dashboard/symptom-alerts")
    public ResponseEntity<List<Map<String, Object>>> getSymptomTrendAlerts() {
        try {
            List<Map<String, Object>> alerts = queueInsightsService.getSymptomTrendAlerts();
            log.info("Retrieved symptom trend alerts: {} alerts", alerts.size());
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            log.error("Error getting symptom trend alerts", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/dashboard/capacity-metrics")
    public ResponseEntity<Map<String, Object>> getCapacityUtilizationMetrics() {
        try {
            Map<String, Object> metrics = queueInsightsService.getCapacityUtilizationMetrics();
            log.info("Retrieved capacity utilization metrics");
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error getting capacity utilization metrics", e);
            return ResponseEntity.ok(Map.of("error", "Failed to fetch capacity metrics"));
        }
    }

    @GetMapping("/dashboard/hourly-performance")
    public ResponseEntity<List<Map<String, Object>>> getHourlyPerformanceBreakdown() {
        try {
            List<Map<String, Object>> performance = queueInsightsService.getHourlyPerformanceBreakdown();
            log.info("Retrieved hourly performance breakdown: {} hours", performance.size());
            return ResponseEntity.ok(performance);
        } catch (Exception e) {
            log.error("Error getting hourly performance breakdown", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/dashboard/overview")
    public ResponseEntity<Map<String, Object>> getComprehensiveDashboardOverview() {
        try {
            Map<String, Object> overview = queueInsightsService.getComprehensiveDashboardOverview();
            log.info("Retrieved comprehensive dashboard overview");
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            log.error("Error getting comprehensive dashboard overview", e);
            return ResponseEntity.ok(Map.of("error", "Failed to fetch comprehensive dashboard overview"));
        }
    }
}
