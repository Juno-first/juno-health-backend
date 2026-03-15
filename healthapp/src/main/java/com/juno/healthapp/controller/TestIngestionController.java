package com.juno.healthapp.controller;

import com.juno.healthapp.dto.CheckInRequest;
import com.juno.healthapp.dto.CheckInResponse;
import com.juno.healthapp.dao.DepartmentDAO;
import com.juno.healthapp.dao.FacilityDAO;
import com.juno.healthapp.dao.PatientDAO;
import com.juno.healthapp.entity.AuthAccount;
import com.juno.healthapp.entity.Department;
import com.juno.healthapp.entity.Facility;
import com.juno.healthapp.entity.Patient;
import com.juno.healthapp.service.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestIngestionController {

    private final QueueService queueService;
    private final DepartmentDAO departmentDAO;
    private final FacilityDAO facilityDAO; // ← add this line
    private final PatientDAO patientDAO; // ← add this



    @PostMapping("/checkin")
    public ResponseEntity<String> testCheckIn(@RequestBody Map<String, Object> request) {
        try {

            Optional<Department> existingDept = departmentDAO.findByQrToken("test-qr-token-12345");
            if (existingDept.isEmpty()) {
                Facility testFacility = Facility.builder()
                    .name("Test Facility")
                    .address("123 Test St")
                    .parish("Kingston")
                    .phone("123-456-7890")
                    .nhfAccepted(false)
                    .build();

                testFacility = facilityDAO.save(testFacility);

                Department testDepartment = Department.builder()
                    .name("Emergency")
                    .departmentType("EMERGENCY")
                    .facility(testFacility)
                    .qrToken("test-qr-token-12345")
                    .checkinCode("TEST123")
                    .isActive(true)
                    .build();

                departmentDAO.save(testDepartment);
                log.info("Created test department with QR token: test-qr-token-12345");
            }

            CheckInRequest checkInRequest = new CheckInRequest(
                "QR",
                "test-qr-token-12345",
                "Routine Checkup",
                "MILD",
                3,
                List.of("headache", "fatigue"),
                "2 days",
                "Patient reports occasional headaches",
                "WALK_IN"
            );

            Patient samplePatient = Patient.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender("MALE")
                .isActive(true)
                .build();

            samplePatient = patientDAO.save(samplePatient); // ← persist before use

            AuthAccount sampleAuth = AuthAccount.builder()
                .id(UUID.randomUUID())
                .accountType("PATIENT")
                .email("john.doe@test.com")
                .username("johndoe")
                .patient(samplePatient)
                .build();

            CheckInResponse response = queueService.checkIn(sampleAuth, checkInRequest);

            log.info("Test check-in completed. Visit ID: {}, Queue Entry ID: {}",
                response.visitId(), response.queueEntryId());

            return ResponseEntity.ok("Check-in test completed. Visit ID: " + response.visitId() +
                ", Queue Entry ID: " + response.queueEntryId());

        } catch (Exception e) {
            log.error("Test check-in failed", e);
            return ResponseEntity.badRequest().body("Check-in test failed: " + e.getMessage());
        }
    }

    @GetMapping("/kafka-topics")
    public ResponseEntity<Map<String, String>> getKafkaTopicInfo() {
        return ResponseEntity.ok(Map.of(
            "patient_queue_topic", "patient_queue - carries detailed QueueUpdateEvent data to ClickHouse",
            "department_topic", "department.queue.snapshot - carries department queue snapshots",
            "ingestion_flow", "checkIn -> QueueUpdateEvent -> patient_queue topic -> Kafka Connect -> ClickHouse"
        ));
    }

    @GetMapping("/clickhouse-schema")
    public ResponseEntity<Map<String, Object>> getClickHouseSchema() {
        return ResponseEntity.ok(Map.of(
            "database", "healthcare_queue",
            "table", "patient_queue",
            "key_fields", List.of("patient_id", "department_id", "visit_id", "queue_entry_id"),
            "enriched_fields", List.of(
                "symptom_severity", "pain_level", "symptom_categories", 
                "ai_priority_score", "estimated_wait_minutes", "priority_tier",
                "assigned_staff_name", "assigned_staff_role", "room_name",
                "event_type", "event_timestamp"
            )
        ));
    }
}
