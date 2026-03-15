package com.juno.healthapp.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class QueueInsightsRepository {

    private final JdbcTemplate clickhouseJdbcTemplate;

    public QueueInsightsRepository(
            @Qualifier("clickHouseJdbcTemplate") JdbcTemplate clickHouseJdbcTemplate) {
        this.clickhouseJdbcTemplate = clickHouseJdbcTemplate;
    }

    public long getTotalPatients() {
        String sql = """
            SELECT count(*) 
            FROM patient_queue 
            WHERE queue_status IN ('waiting', 'being_assessed')
            """;
        
        try {
            Long result = clickhouseJdbcTemplate.queryForObject(sql, Long.class);
            return result != null ? result : 0L;
        } catch (Exception e) {
            log.error("Error getting total patients: {}", e.getMessage());
            return 0L;
        }
    }

    public long getHighPriorityPatients() {
        String sql = """
            SELECT count(*) 
            FROM patient_queue 
            WHERE queue_status IN ('waiting', 'being_assessed') 
            AND priority_level <= 2
            """;
        
        try {
            Long result = clickhouseJdbcTemplate.queryForObject(sql, Long.class);
            return result != null ? result : 0L;
        } catch (Exception e) {
            log.error("Error getting high priority patients: {}", e.getMessage());
            return 0L;
        }
    }

    public double getAverageWaitTime() {
        String sql = """
            SELECT avg(toUnixTimestamp(now()) - toUnixTimestamp(created_at)) as avg_wait_seconds
            FROM patient_queue 
            WHERE queue_status IN ('waiting', 'being_assessed')
            """;
        
        try {
            Double result = clickhouseJdbcTemplate.queryForObject(sql, Double.class);
            return result != null ? result : 0.0;
        } catch (Exception e) {
            log.error("Error getting average wait time: {}", e.getMessage());
            return 0.0;
        }
    }

    public long getPatientsProcessedToday() {
        String sql = """
            SELECT count(*) 
            FROM patient_queue 
            WHERE queue_status = 'processed' 
            AND toDate(created_at) = today()
            """;
        
        try {
            Long result = clickhouseJdbcTemplate.queryForObject(sql, Long.class);
            return result != null ? result : 0L;
        } catch (Exception e) {
            log.error("Error getting patients processed today: {}", e.getMessage());
            return 0L;
        }
    }

    public Map<String, Object> getAllInsights() {
        String sql = """
            SELECT 
                count(*) as total_patients,
                countIf(priority_level <= 2) as high_priority_patients,
                avg(toUnixTimestamp(now()) - toUnixTimestamp(created_at)) as avg_wait_seconds,
                countIf(queue_status = 'processed' AND toDate(created_at) = today()) as patients_processed
            FROM patient_queue 
            WHERE queue_status IN ('waiting', 'being_assessed', 'processed')
            """;
        
        try {
            return clickhouseJdbcTemplate.queryForMap(sql);
        } catch (Exception e) {
            log.error("Error getting all insights: {}", e.getMessage());
            return Map.of(
                "total_patients", 0L,
                "high_priority_patients", 0L,
                "avg_wait_seconds", 0.0,
                "patients_processed", 0L
            );
        }
    }

    // Detailed insights using enriched patient_queue data
    
    public List<Map<String, Object>> getAverageWaitTimeBySymptomSeverity() {
        String sql = """
            SELECT 
                symptom_severity,
                AVG(estimated_wait_minutes) as avg_wait_minutes,
                COUNT(*) as patient_count
            FROM patient_queue 
            WHERE event_type = 'CHECKED_IN' 
            AND symptom_severity IS NOT NULL
            GROUP BY symptom_severity
            ORDER BY avg_wait_minutes
            """;
        
        try {
            return clickhouseJdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("Error getting average wait time by symptom severity: {}", e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> getDepartmentPerformanceMetrics() {
        String sql = """
            SELECT 
                department_id,
                COUNT(*) as patients,
                AVG(ai_priority_score) as avg_priority_score,
                AVG(estimated_wait_minutes) as avg_wait_minutes,
                COUNTIf(priority_tier = 'HIGH') as high_priority_count,
                COUNTIf(priority_tier = 'MEDIUM') as medium_priority_count,
                COUNTIf(priority_tier = 'LOW') as low_priority_count
            FROM patient_queue 
            WHERE event_timestamp >= today() - 7
            AND event_type = 'CHECKED_IN'
            GROUP BY department_id
            ORDER BY patients DESC
            """;
        
        try {
            return clickhouseJdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("Error getting department performance metrics: {}", e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> getPainLevelAnalysis() {
        String sql = """
            SELECT 
                pain_level,
                COUNT(*) as patient_count,
                AVG(ai_priority_score) as avg_priority_score,
                AVG(estimated_wait_minutes) as avg_wait_minutes,
                COUNTIf(symptom_severity = 'SEVERE') as severe_symptoms_count
            FROM patient_queue 
            WHERE pain_level IS NOT NULL
            AND event_type = 'CHECKED_IN'
            GROUP BY pain_level
            ORDER BY pain_level
            """;
        
        try {
            return clickhouseJdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("Error getting pain level analysis: {}", e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> getSymptomCategoryAnalysis() {
        String sql = """
            SELECT 
                arrayJoin(symptom_categories) as symptom_category,
                COUNT(*) as patient_count,
                AVG(ai_priority_score) as avg_priority_score,
                AVG(estimated_wait_minutes) as avg_wait_minutes,
                COUNTIf(priority_tier = 'HIGH') as high_priority_count
            FROM patient_queue 
            WHERE event_type = 'CHECKED_IN'
            AND length(symptom_categories) > 0
            GROUP BY symptom_category
            ORDER BY patient_count DESC
            """;
        
        try {
            return clickhouseJdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("Error getting symptom category analysis: {}", e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> getHourlyQueueTrends() {
        String sql = """
            SELECT 
                toHour(event_timestamp) as hour_of_day,
                COUNT(*) as patient_count,
                AVG(estimated_wait_minutes) as avg_wait_minutes,
                AVG(ai_priority_score) as avg_priority_score,
                COUNTIf(priority_tier = 'HIGH') as high_priority_count
            FROM patient_queue 
            WHERE event_type = 'CHECKED_IN'
            AND toDate(event_timestamp) >= today() - 7
            GROUP BY hour_of_day
            ORDER BY hour_of_day
            """;
        
        try {
            return clickhouseJdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("Error getting hourly queue trends: {}", e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> getStaffAssignmentAnalytics() {
        String sql = """
            SELECT 
                assigned_staff_name,
                assigned_staff_role,
                COUNT(*) as patients_seen,
                AVG(ai_priority_score) as avg_priority_score,
                COUNTIf(symptom_severity = 'SEVERE') as severe_cases_handled
            FROM patient_queue 
            WHERE event_type IN ('CALLED', 'DISCHARGED')
            AND assigned_staff_name IS NOT NULL
            AND event_timestamp >= today() - 7
            GROUP BY assigned_staff_name, assigned_staff_role
            ORDER BY patients_seen DESC
            """;
        
        try {
            return clickhouseJdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("Error getting staff assignment analytics: {}", e.getMessage());
            return List.of();
        }
    }

    public Map<String, Object> getComprehensiveInsights() {
        String sql = """
            SELECT 
                COUNT(*) as total_checkins,
                COUNTIf(event_type = 'CHECKED_IN') as checkins_today,
                COUNTIf(event_type = 'CALLED') as called_today,
                COUNTIf(event_type = 'DISCHARGED') as discharged_today,
                AVG(estimated_wait_minutes) as avg_wait_time,
                AVG(ai_priority_score) as avg_priority_score,
                COUNTIf(priority_tier = 'HIGH') as high_priority_patients,
                COUNTIf(symptom_severity = 'SEVERE') as severe_symptom_patients,
                COUNTIf(pain_level >= 7) as high_pain_patients,
                COUNTIf(length(symptom_categories) > 2) as multiple_symptom_patients
            FROM patient_queue 
            WHERE toDate(event_timestamp) = today()
            """;
        
        try {
            return clickhouseJdbcTemplate.queryForMap(sql);
        } catch (Exception e) {
            log.error("Error getting comprehensive insights: {}", e.getMessage());
            return Map.of(
                "total_checkins", 0L,
                "checkins_today", 0L,
                "called_today", 0L,
                "discharged_today", 0L,
                "avg_wait_time", 0.0,
                "avg_priority_score", 0.0,
                "high_priority_patients", 0L,
                "severe_symptom_patients", 0L,
                "high_pain_patients", 0L,
                "multiple_symptom_patients", 0L
            );
        }
    }

    // Historical trend analysis methods
    
    public List<Map<String, Object>> getDailyPatientVolumeTrends(int days) {
        String sql = String.format("""
            SELECT 
                toDate(event_timestamp) as date,
                COUNTIf(event_type = 'CHECKED_IN') as daily_checkins,
                COUNTIf(event_type = 'CALLED') as daily_called,
                COUNTIf(event_type = 'DISCHARGED') as daily_discharged,
                AVG(estimated_wait_minutes) as avg_wait_time,
                AVG(ai_priority_score) as avg_priority_score,
                COUNTIf(priority_tier = 'HIGH') as high_priority_count,
                COUNTIf(symptom_severity = 'SEVERE') as severe_cases_count
            FROM patient_queue 
            WHERE toDate(event_timestamp) >= today() - %d
            GROUP BY date
            ORDER BY date
            """, days);
        
        try {
            return clickhouseJdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("Error getting daily patient volume trends: {}", e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> getWeeklyDepartmentTrends(int weeks) {
        String sql = String.format("""
            SELECT 
                toStartOfWeek(event_timestamp) as week_start,
                department_id,
                COUNT(*) as weekly_patients,
                AVG(estimated_wait_minutes) as avg_wait_time,
                AVG(ai_priority_score) as avg_priority_score,
                COUNTIf(priority_tier = 'HIGH') as high_priority_count,
                COUNTIf(symptom_severity = 'SEVERE') as severe_cases_count,
                COUNTIf(pain_level >= 7) as high_pain_count
            FROM patient_queue 
            WHERE event_type = 'CHECKED_IN'
            AND toStartOfWeek(event_timestamp) >= toStartOfWeek(today()) - INTERVAL %d WEEK
            GROUP BY week_start, department_id
            ORDER BY week_start DESC, weekly_patients DESC
            """, weeks);
        
        try {
            return clickhouseJdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("Error getting weekly department trends: {}", e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> getMonthlySymptomTrends(int months) {
        String sql = String.format("""
            SELECT 
                toStartOfMonth(event_timestamp) as month_start,
                symptom_severity,
                COUNT(*) as patient_count,
                AVG(estimated_wait_minutes) as avg_wait_time,
                AVG(ai_priority_score) as avg_priority_score,
                AVG(pain_level) as avg_pain_level,
                COUNTIf(priority_tier = 'HIGH') as high_priority_count
            FROM patient_queue 
            WHERE event_type = 'CHECKED_IN'
            AND symptom_severity IS NOT NULL
            AND toStartOfMonth(event_timestamp) >= toStartOfMonth(today()) - INTERVAL %d MONTH
            GROUP BY month_start, symptom_severity
            ORDER BY month_start DESC, patient_count DESC
            """, months);
        
        try {
            return clickhouseJdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("Error getting monthly symptom trends: {}", e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> getHourlyWaitTimeTrends(int days) {
        String sql = String.format("""
            SELECT 
                toHour(event_timestamp) as hour_of_day,
                toDayOfWeek(event_timestamp) as day_of_week,
                AVG(estimated_wait_minutes) as avg_wait_time,
                COUNT(*) as patient_count,
                quantile(0.5)(estimated_wait_minutes) as median_wait_time,
                quantile(0.95)(estimated_wait_minutes) as p95_wait_time,
                AVG(ai_priority_score) as avg_priority_score
            FROM patient_queue 
            WHERE event_type = 'CHECKED_IN'
            AND toDate(event_timestamp) >= today() - %d
            GROUP BY hour_of_day, day_of_week
            ORDER BY day_of_week, hour_of_day
            """, days);
        
        try {
            return clickhouseJdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("Error getting hourly wait time trends: {}", e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> getStaffPerformanceTrends(int days) {
        String sql = String.format("""
            SELECT 
                assigned_staff_name,
                assigned_staff_role,
                toDate(event_timestamp) as date,
                COUNT(*) as patients_seen,
                AVG(ai_priority_score) as avg_priority_score,
                COUNTIf(symptom_severity = 'SEVERE') as severe_cases,
                COUNTIf(pain_level >= 7) as high_pain_cases,
                AVG(estimated_wait_minutes) as avg_wait_before_service
            FROM patient_queue 
            WHERE event_type IN ('CALLED', 'DISCHARGED')
            AND assigned_staff_name IS NOT NULL
            AND toDate(event_timestamp) >= today() - %d
            GROUP BY assigned_staff_name, assigned_staff_role, date
            ORDER BY date DESC, patients_seen DESC
            """, days);
        
        try {
            return clickhouseJdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("Error getting staff performance trends: {}", e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> getQueueDepthTrends(int days) {
        String sql = String.format("""
            SELECT 
                toHour(event_timestamp) as hour_of_day,
                toDate(event_timestamp) as date,
                AVG(queue_depth) as avg_queue_depth,
                MAX(queue_depth) as max_queue_depth,
                COUNT(*) as events,
                AVG(position) as avg_position_in_queue
            FROM patient_queue 
            WHERE event_type = 'CHECKED_IN'
            AND queue_depth IS NOT NULL
            AND toDate(event_timestamp) >= today() - %d
            GROUP BY hour_of_day, date
            ORDER BY date DESC, hour_of_day
            """, days);
        
        try {
            return clickhouseJdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("Error getting queue depth trends: {}", e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> getSymptomCategoryEvolution(int months) {
        String sql = String.format("""
            SELECT 
                toStartOfMonth(event_timestamp) as month_start,
                arrayJoin(symptom_categories) as symptom_category,
                COUNT(*) as patient_count,
                AVG(ai_priority_score) as avg_priority_score,
                AVG(estimated_wait_minutes) as avg_wait_time,
                COUNTIf(symptom_severity = 'SEVERE') as severe_cases,
                COUNTIf(pain_level >= 7) as high_pain_cases,
                quantile(0.5)(ai_priority_score) as median_priority_score
            FROM patient_queue 
            WHERE event_type = 'CHECKED_IN'
            AND length(symptom_categories) > 0
            AND toStartOfMonth(event_timestamp) >= toStartOfMonth(today()) - INTERVAL %d MONTH
            GROUP BY month_start, symptom_category
            ORDER BY month_start DESC, patient_count DESC
            """, months);
        
        try {
            return clickhouseJdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("Error getting symptom category evolution: {}", e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> getPriorityScoreDistributionTrends(int days) {
        String sql = String.format("""
            SELECT 
                toDate(event_timestamp) as date,
                quantile(0.25)(ai_priority_score) as q25_priority,
                quantile(0.5)(ai_priority_score) as median_priority,
                quantile(0.75)(ai_priority_score) as q75_priority,
                quantile(0.95)(ai_priority_score) as q95_priority,
                AVG(ai_priority_score) as avg_priority,
                COUNT(*) as total_patients,
                COUNTIf(priority_tier = 'HIGH') as high_priority_count,
                COUNTIf(priority_tier = 'LOW') as low_priority_count
            FROM patient_queue 
            WHERE event_type = 'CHECKED_IN'
            AND ai_priority_score IS NOT NULL
            AND toDate(event_timestamp) >= today() - %d
            GROUP BY date
            ORDER BY date DESC
            """, days);
        
        try {
            return clickhouseJdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("Error getting priority score distribution trends: {}", e.getMessage());
            return List.of();
        }
    }

    public Map<String, Object> getHistoricalComparisons() {
        String sql = """
            SELECT 
                'today' as period,
                COUNTIf(event_type = 'CHECKED_IN' AND toDate(event_timestamp) = today()) as checkins,
                COUNTIf(event_type = 'DISCHARGED' AND toDate(event_timestamp) = today()) as discharges,
                AVGIf(estimated_wait_minutes, event_type = 'CHECKED_IN' AND toDate(event_timestamp) = today()) as avg_wait_today,
                AVGIf(ai_priority_score, event_type = 'CHECKED_IN' AND toDate(event_timestamp) = today()) as avg_priority_today
            FROM patient_queue 
            WHERE toDate(event_timestamp) >= today() - 1
            
            UNION ALL
            
            SELECT 
                'yesterday' as period,
                COUNTIf(event_type = 'CHECKED_IN' AND toDate(event_timestamp) = yesterday()) as checkins,
                COUNTIf(event_type = 'DISCHARGED' AND toDate(event_timestamp) = yesterday()) as discharges,
                AVGIf(estimated_wait_minutes, event_type = 'CHECKED_IN' AND toDate(event_timestamp) = yesterday()) as avg_wait_today,
                AVGIf(ai_priority_score, event_type = 'CHECKED_IN' AND toDate(event_timestamp) = yesterday()) as avg_priority_today
            FROM patient_queue 
            WHERE toDate(event_timestamp) >= yesterday() - 1
            
            UNION ALL
            
            SELECT 
                'last_week' as period,
                COUNTIf(event_type = 'CHECKED_IN' AND toDate(event_timestamp) >= today() - 7 AND toDate(event_timestamp) < today()) as checkins,
                COUNTIf(event_type = 'DISCHARGED' AND toDate(event_timestamp) >= today() - 7 AND toDate(event_timestamp) < today()) as discharges,
                AVGIf(estimated_wait_minutes, event_type = 'CHECKED_IN' AND toDate(event_timestamp) >= today() - 7 AND toDate(event_timestamp) < today()) as avg_wait_today,
                AVGIf(ai_priority_score, event_type = 'CHECKED_IN' AND toDate(event_timestamp) >= today() - 7 AND toDate(event_timestamp) < today()) as avg_priority_today
            FROM patient_queue 
            WHERE toDate(event_timestamp) >= today() - 7
            """;
        
        try {
            List<Map<String, Object>> results = clickhouseJdbcTemplate.queryForList(sql);
            Map<String, Object> comparisons = new java.util.HashMap<>();
            for (Map<String, Object> row : results) {
                comparisons.put(row.get("period").toString(), row);
            }
            return comparisons;
        } catch (Exception e) {
            log.error("Error getting historical comparisons: {}", e.getMessage());
            return Map.of("error", "Failed to fetch historical comparisons");
        }
    }

    // Dashboard-specific real-time insights
    
    public Map<String, Object> getRealtimeDashboardMetrics() {
        String sql = """
            SELECT 
                -- Current Queue Status
                COUNTIf(event_type = 'CHECKED_IN' AND status IN ('waiting', 'being_assessed')) as currently_waiting,
                COUNTIf(event_type = 'CALLED' AND status = 'in_progress') as currently_being_seen,
                COUNTIf(event_type = 'CHECKED_IN' AND priority_tier = 'HIGH' AND status IN ('waiting', 'being_assessed')) as high_priority_waiting,
                
                -- Today's Performance
                COUNTIf(event_type = 'CHECKED_IN' AND toDate(event_timestamp) = today()) as total_checkins_today,
                COUNTIf(event_type = 'DISCHARGED' AND toDate(event_timestamp) = today()) as total_discharges_today,
                AVGIf(estimated_wait_minutes, event_type = 'CHECKED_IN' AND toDate(event_timestamp) = today()) as avg_wait_today,
                AVGIf(ai_priority_score, event_type = 'CHECKED_IN' AND toDate(event_timestamp) = today()) as avg_priority_today,
                
                -- Critical Cases
                COUNTIf(symptom_severity = 'SEVERE' AND status IN ('waiting', 'being_assessed')) as severe_cases_waiting,
                COUNTIf(pain_level >= 8 AND status IN ('waiting', 'being_assessed')) as high_pain_waiting,
                COUNTIf(ai_priority_score >= 80 AND status IN ('waiting', 'being_assessed')) as critical_ai_score_waiting,
                
                -- Wait Time Percentiles
                quantileIf(0.5, estimated_wait_minutes, event_type = 'CHECKED_IN' AND toDate(event_timestamp) = today()) as median_wait_today,
                quantileIf(0.95, estimated_wait_minutes, event_type = 'CHECKED_IN' AND toDate(event_timestamp) = today()) as p95_wait_today,
                quantileIf(0.5, toUnixTimestamp(now()) - toUnixTimestamp(checked_in_at), status IN ('waiting', 'being_assessed')) as median_current_wait,
                
                -- Department Load
                COUNTIf(department_id = 'ER' AND status IN ('waiting', 'being_assessed')) as er_waiting,
                COUNTIf(department_id = 'URGENT_CARE' AND status IN ('waiting', 'being_assessed')) as urgent_care_waiting,
                COUNTIf(department_id = 'PRIMARY_CARE' AND status IN ('waiting', 'being_assessed')) as primary_care_waiting,
                
                -- Staff Availability
                COUNT(DISTINCT assigned_staff_name) as staff_active_today,
                COUNTIf(event_type = 'DISCHARGED' AND toDate(event_timestamp) = today()) as patients_per_staff_avg,
                
                -- Peak Hour Analysis
                COUNTIf(event_type = 'CHECKED_IN' AND toHour(event_timestamp) BETWEEN 8 AND 12 AND toDate(event_timestamp) = today()) as morning_checkins,
                COUNTIf(event_type = 'CHECKED_IN' AND toHour(event_timestamp) BETWEEN 12 AND 16 AND toDate(event_timestamp) = today()) as afternoon_checkins,
                COUNTIf(event_type = 'CHECKED_IN' AND toHour(event_timestamp) BETWEEN 16 AND 20 AND toDate(event_timestamp) = today()) as evening_checkins
                
            FROM patient_queue 
            WHERE event_timestamp >= today() - INTERVAL 1 DAY
            """;
        
        try {
            return clickhouseJdbcTemplate.queryForMap(sql);
        } catch (Exception e) {
            log.error("Error getting realtime dashboard metrics: {}", e.getMessage());
            return Map.of("error", "Failed to fetch dashboard metrics");
        }
    }

    public List<Map<String, Object>> getDepartmentLoadAnalysis() {
        String sql = """
            SELECT 
                department_id,
                COUNTIf(status IN ('waiting', 'being_assessed')) as current_patients,
                COUNTIf(event_type = 'CHECKED_IN' AND toDate(event_timestamp) = today()) as today_checkins,
                AVGIf(estimated_wait_minutes, event_type = 'CHECKED_IN' AND toDate(event_timestamp) = today()) as avg_wait_time,
                COUNTIf(priority_tier = 'HIGH' AND status IN ('waiting', 'being_assessed')) as high_priority_waiting,
                COUNTIf(symptom_severity = 'SEVERE' AND status IN ('waiting', 'being_assessed')) as severe_cases_waiting,
                AVGIf(ai_priority_score, event_type = 'CHECKED_IN' AND toDate(event_timestamp) = today()) as avg_priority_score,
                COUNTIf(pain_level >= 7 AND status IN ('waiting', 'being_assessed')) as high_pain_cases,
                COUNT(DISTINCT assigned_staff_name) as staff_count,
                round(COUNTIf(status IN ('waiting', 'being_assessed')) * 1.0 / NULLIF(COUNT(DISTINCT assigned_staff_name), 0), 2) as patients_per_staff
            FROM patient_queue 
            WHERE event_timestamp >= today() - INTERVAL 1 DAY
            GROUP BY department_id
            ORDER BY current_patients DESC
            """;
        
        try {
            return clickhouseJdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("Error getting department load analysis: {}", e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> getStaffUtilizationMetrics() {
        String sql = """
            SELECT 
                assigned_staff_name,
                assigned_staff_role,
                COUNTIf(event_type = 'CALLED' AND toDate(event_timestamp) = today()) as patients_called_today,
                COUNTIf(event_type = 'DISCHARGED' AND toDate(event_timestamp) = today()) as patients_discharged_today,
                AVGIf(ai_priority_score, event_type = 'CALLED' AND toDate(event_timestamp) = today()) as avg_priority_handled,
                COUNTIf(symptom_severity = 'SEVERE', event_type = 'DISCHARGED' AND toDate(event_timestamp) = today()) as severe_cases_handled,
                COUNTIf(pain_level >= 7, event_type = 'DISCHARGED' AND toDate(event_timestamp) = today()) as high_pain_cases_handled,
                AVGIf(estimated_wait_minutes, event_type = 'CALLED' AND toDate(event_timestamp) = today()) as avg_wait_before_call,
                MIN(toHour(event_timestamp), event_type = 'CALLED' AND toDate(event_timestamp) = today()) as first_call_hour,
                MAX(toHour(event_timestamp), event_type = 'DISCHARGED' AND toDate(event_timestamp) = today()) as last_discharge_hour
            FROM patient_queue 
            WHERE assigned_staff_name IS NOT NULL
            AND toDate(event_timestamp) = today()
            GROUP BY assigned_staff_name, assigned_staff_role
            ORDER BY patients_discharged_today DESC
            """;
        
        try {
            return clickhouseJdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("Error getting staff utilization metrics: {}", e.getMessage());
            return List.of();
        }
    }

    public Map<String, Object> getWaitTimeAnalysis() {
        String sql = """
            SELECT 
                -- Current wait times by priority
                AVGIf(toUnixTimestamp(now()) - toUnixTimestamp(checked_in_at), priority_tier = 'HIGH' AND status IN ('waiting', 'being_assessed')) as high_priority_avg_wait,
                AVGIf(toUnixTimestamp(now()) - toUnixTimestamp(checked_in_at), priority_tier = 'MEDIUM' AND status IN ('waiting', 'being_assessed')) as medium_priority_avg_wait,
                AVGIf(toUnixTimestamp(now()) - toUnixTimestamp(checked_in_at), priority_tier = 'LOW' AND status IN ('waiting', 'being_assessed')) as low_priority_avg_wait,
                
                -- Wait time by severity
                AVGIf(toUnixTimestamp(now()) - toUnixTimestamp(checked_in_at), symptom_severity = 'SEVERE' AND status IN ('waiting', 'being_assessed')) as severe_avg_wait,
                AVGIf(toUnixTimestamp(now()) - toUnixTimestamp(checked_in_at), symptom_severity = 'MODERATE' AND status IN ('waiting', 'being_assessed')) as moderate_avg_wait,
                AVGIf(toUnixTimestamp(now()) - toUnixTimestamp(checked_in_at), symptom_severity = 'MILD' AND status IN ('waiting', 'being_assessed')) as mild_avg_wait,
                
                -- Historical comparison
                AVGIf(estimated_wait_minutes, event_type = 'CHECKED_IN' AND toDate(event_timestamp) = today()) as avg_estimated_wait_today,
                AVGIf(estimated_wait_minutes, event_type = 'CHECKED_IN' AND toDate(event_timestamp) = yesterday()) as avg_estimated_wait_yesterday,
                AVGIf(estimated_wait_minutes, event_type = 'CHECKED_IN' AND toDate(event_timestamp) >= today() - 7) as avg_estimated_wait_week,
                
                -- Wait time breaches
                COUNTIf(estimated_wait_minutes > 60 AND event_type = 'CHECKED_IN' AND toDate(event_timestamp) = today()) as wait_over_60min,
                COUNTIf(estimated_wait_minutes > 120 AND event_type = 'CHECKED_IN' AND toDate(event_timestamp) = today()) as wait_over_120min,
                COUNTIf(toUnixTimestamp(now()) - toUnixTimestamp(checked_in_at) > 3600 AND status IN ('waiting', 'being_assessed')) as waiting_over_1hr,
                COUNTIf(toUnixTimestamp(now()) - toUnixTimestamp(checked_in_at) > 7200 AND status IN ('waiting', 'being_assessed')) as waiting_over_2hr
                
            FROM patient_queue 
            WHERE event_timestamp >= today() - INTERVAL 7 DAY
            """;
        
        try {
            return clickhouseJdbcTemplate.queryForMap(sql);
        } catch (Exception e) {
            log.error("Error getting wait time analysis: {}", e.getMessage());
            return Map.of("error", "Failed to fetch wait time analysis");
        }
    }

    public List<Map<String, Object>> getSymptomTrendAlerts() {
        String sql = """
            SELECT 
                symptom_severity,
                COUNTIf(status IN ('waiting', 'being_assessed')) as currently_waiting,
                COUNTIf(event_type = 'CHECKED_IN' AND toDate(event_timestamp) = today()) as today_cases,
                COUNTIf(event_type = 'CHECKED_IN' AND toDate(event_timestamp) = yesterday()) as yesterday_cases,
                round(((COUNTIf(event_type = 'CHECKED_IN' AND toDate(event_timestamp) = today()) * 1.0) / 
                      NULLIF(COUNTIf(event_type = 'CHECKED_IN' AND toDate(event_timestamp) = yesterday()), 0) - 1) * 100, 2) as day_over_day_change,
                AVGIf(ai_priority_score, event_type = 'CHECKED_IN' AND toDate(event_timestamp) = today()) as avg_priority_today,
                COUNTIf(pain_level >= 7 AND status IN ('waiting', 'being_assessed')) as high_pain_waiting,
                AVGIf(toUnixTimestamp(now()) - toUnixTimestamp(checked_in_at), status IN ('waiting', 'being_assessed')) as avg_wait_minutes
            FROM patient_queue 
            WHERE event_timestamp >= today() - INTERVAL 2 DAY
            AND symptom_severity IS NOT NULL
            GROUP BY symptom_severity
            HAVING currently_waiting > 0 OR today_cases > 0
            ORDER BY currently_waiting DESC, today_cases DESC
            """;
        
        try {
            return clickhouseJdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("Error getting symptom trend alerts: {}", e.getMessage());
            return List.of();
        }
    }

    public Map<String, Object> getCapacityUtilizationMetrics() {
        String sql = """
            SELECT 
                -- Overall capacity metrics
                COUNTIf(event_type = 'CHECKED_IN' AND toDate(event_timestamp) = today()) as total_capacity_used,
                COUNTIf(event_type = 'CHECKED_IN' AND toHour(event_timestamp) = toHour(now()) AND toDate(event_timestamp) = today()) as current_hour_capacity,
                
                -- Peak capacity analysis
                MAX(COUNTIf(event_type = 'CHECKED_IN', toHour(event_timestamp))) as peak_hourly_checkins,
                argMax(toHour(event_timestamp), COUNTIf(event_type = 'CHECKED_IN', toHour(event_timestamp))) as peak_hour,
                
                -- Room utilization
                COUNT(DISTINCT room_name) as rooms_used_today,
                COUNTIf(room_name IS NOT NULL AND status IN ('waiting', 'being_assessed')) as rooms_currently_occupied,
                
                -- Queue efficiency
                COUNTIf(event_type = 'CHECKED_IN' AND toDate(event_timestamp) = today()) * 1.0 / 
                NULLIF(COUNT(DISTINCT assigned_staff_name), 0) as patients_per_staff_ratio,
                COUNTIf(event_type = 'DISCHARGED' AND toDate(event_timestamp) = today()) * 1.0 / 
                NULLIF(COUNTIf(event_type = 'CHECKED_IN' AND toDate(event_timestamp) = today()), 0) as completion_rate,
                
                -- Average processing time
                AVGIf(toUnixTimestamp(updated_at) - toUnixTimestamp(checked_in_at), 
                      event_type = 'DISCHARGED' AND toDate(event_timestamp) = today()) as avg_processing_time_minutes,
                      
                -- No-show and abandonment rates
                COUNTIf(status = 'ABANDONED' AND toDate(event_timestamp) = today()) as abandoned_today,
                COUNTIf(status = 'NO_SHOW' AND toDate(event_timestamp) = today()) as no_show_today
                
            FROM patient_queue 
            WHERE event_timestamp >= today() - INTERVAL 1 DAY
            """;
        
        try {
            return clickhouseJdbcTemplate.queryForMap(sql);
        } catch (Exception e) {
            log.error("Error getting capacity utilization metrics: {}", e.getMessage());
            return Map.of("error", "Failed to fetch capacity metrics");
        }
    }

    public List<Map<String, Object>> getHourlyPerformanceBreakdown() {
        String sql = """
            SELECT 
                toHour(event_timestamp) as hour_of_day,
                COUNTIf(event_type = 'CHECKED_IN' AND toDate(event_timestamp) = today()) as checkins,
                COUNTIf(event_type = 'CALLED' AND toDate(event_timestamp) = today()) as calls,
                COUNTIf(event_type = 'DISCHARGED' AND toDate(event_timestamp) = today()) as discharges,
                AVGIf(estimated_wait_minutes, event_type = 'CHECKED_IN' AND toDate(event_timestamp) = today()) as avg_estimated_wait,
                AVGIf(ai_priority_score, event_type = 'CHECKED_IN' AND toDate(event_timestamp) = today()) as avg_priority_score,
                COUNTIf(priority_tier = 'HIGH' AND event_type = 'CHECKED_IN' AND toDate(event_timestamp) = today()) as high_priority_checkins,
                COUNTIf(symptom_severity = 'SEVERE' AND event_type = 'CHECKED_IN' AND toDate(event_timestamp) = today()) as severe_cases,
                COUNTIf(status IN ('waiting', 'being_assessed') AND toHour(now()) = toHour(event_timestamp)) as currently_waiting_this_hour
            FROM patient_queue 
            WHERE event_timestamp >= today() - INTERVAL 1 DAY
            GROUP BY hour_of_day
            ORDER BY hour_of_day
            """;
        
        try {
            return clickhouseJdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("Error getting hourly performance breakdown: {}", e.getMessage());
            return List.of();
        }
    }
}
