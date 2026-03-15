package com.juno.healthapp.service;

import com.juno.healthapp.dao.*;
import com.juno.healthapp.dto.*;
import com.juno.healthapp.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class QueueService {

    private final DepartmentDAO departmentDAO;
    private final VisitDAO visitDAO;
    private final StaffDAO staffDAO;
    private final QueueEntryDAO queueEntryDAO;
    private final QueueNotificationService notificationService;
    private final QueueOverrideDAO queueOverrideDAO;
    private final RoomDAO roomDAO;
    private final QueueKafkaPublisher queueKafkaPublisher;
    private final PriorityScoreService priorityScoreService;

    private static final int AVG_CONSULTATION_MINUTES = 15;

    @Transactional
    public CheckInResponse checkIn(AuthAccount account, CheckInRequest request) {
        Patient patient = account.getPatient();

        if (patient == null) {
            throw new IllegalStateException("No patient profile linked to this account");
        }

        visitDAO.findActiveVisitByPatient(patient).ifPresent(v -> {
            throw new IllegalStateException("Patient already has an active visit");
        });

        Department department = resolveDepartment(request);
        Facility facility = department.getFacility();

        Visit visit = Visit.builder()
                .patient(patient)
                .facility(facility)
                .department(department)
                .checkedInAt(LocalDateTime.now())
                .presentingComplaint(request.presentingComplaint())
                .visitType(request.visitType())
                .status("CHECKED_IN")
                .build();
        visit = visitDAO.save(visit);

        // ── Score + tier ──────────────────────────────────────────────────────
        ScoreResult scoreResult = priorityScoreService.calculate(
                request.symptomSeverity(),
                request.painLevel(),
                request.symptomCategories(),
                request.symptomDuration()
        );
        int score = scoreResult.score();
        String priorityTier = resolvePriorityTier(score);

        // ── Find correct insert position ──────────────────────────────────────
        // Get current queue ordered by position
        List<QueueEntry> currentQueue = queueEntryDAO.findByDepartmentOrdered(department);

        // Insert after the last entry with an equal or higher score,
        // before the first entry with a lower score
        int insertPosition = currentQueue.stream()
                .filter(e -> e.getAiPriorityScore() >= score)
                .mapToInt(QueueEntry::getPosition)
                .max()
                .orElse(0) + 1;

        // Shift everyone from insertPosition downward by 1
        if (insertPosition <= currentQueue.size()) {
            queueEntryDAO.incrementPositionsFrom(department, insertPosition);
        }

        int queueDepth = queueEntryDAO.countActiveInDepartment(department);

        QueueEntry queueEntry = QueueEntry.builder()
                .visit(visit)
                .patient(patient)
                .department(department)
                .position(insertPosition)
                .aiPriorityScore(score)
                .priorityTier(priorityTier)
                .symptomSeverity(request.symptomSeverity())
                .painLevel(request.painLevel())
                .symptomCategories(request.symptomCategories())
                .symptomDuration(request.symptomDuration())
                .additionalNotes(request.additionalNotes())
                .scoreBreakdown(scoreResult.breakdown())
                .lastScoredAt(LocalDateTime.now())
                .lastPositionUpdatedAt(LocalDateTime.now())
                .build();

        queueEntry = queueEntryDAO.save(queueEntry);

        int estimatedWait = (insertPosition - 1) * AVG_CONSULTATION_MINUTES;

        // ── Notify patients who got bumped down ───────────────────────────────
        LocalDateTime now = LocalDateTime.now();
        for (QueueEntry existing : currentQueue) {
            if (existing.getPosition() >= insertPosition) {
                int newPos = existing.getPosition() + 1;
                int bumpedWait = (newPos - 1) * AVG_CONSULTATION_MINUTES;

                QueueUpdateEvent bumpedEvent = new QueueUpdateEvent(
                        department.getId(),
                        existing.getPatient().getId(),
                        existing.getVisit().getId(),
                        existing.getId(),
                        existing.getPatient().getFullName(),
                        newPos,
                        queueDepth + 1,
                        existing.getPriorityTier(),
                        existing.getAiPriorityScore(),
                        bumpedWait,
                        existing.getSymptomSeverity(),
                        existing.getPainLevel(),
                        existing.getSymptomCategories(),
                        existing.getSymptomDuration(),
                        existing.getVisit().getPresentingComplaint(),
                        existing.getAdditionalNotes(),
                        existing.getVisit().getStatus(),
                        existing.getVisit().getCheckedInAt(),
                        null, null, null,
                        "QUEUE_UPDATED"
                );

                notificationService.notifyPatient(existing.getPatient().getId(), bumpedEvent);
            }
        }

        QueueUpdateEvent event = new QueueUpdateEvent(
                department.getId(),
                patient.getId(),
                visit.getId(),
                queueEntry.getId(),
                patient.getFullName(),
                insertPosition,
                queueDepth + 1,
                priorityTier,
                score,
                estimatedWait,
                queueEntry.getSymptomSeverity(),
                queueEntry.getPainLevel(),
                queueEntry.getSymptomCategories(),
                queueEntry.getSymptomDuration(),
                visit.getPresentingComplaint(),
                queueEntry.getAdditionalNotes(),
                visit.getStatus(),
                visit.getCheckedInAt(),
                null, null, null,
                "CHECKED_IN"
        );

        publishDepartmentSnapshot(department, "CHECKED_IN");
        notificationService.notifyPatient(patient.getId(), event);
        notificationService.notifyDepartmentQueue(department.getId(), event);

        return new CheckInResponse(
                visit.getId(),
                queueEntry.getId(),
                insertPosition,
                queueDepth + 1,
                priorityTier,
                score,
                estimatedWait,
                facility.getName(),
                department.getName(),
                visit.getStatus(),
                department.getQrToken(),
                department.getCheckinCode(),
                visit.getCheckedInAt(),
                null, null, null
        );
    }

    private Department resolveDepartment(CheckInRequest request) {
        if ("QR".equalsIgnoreCase(request.method())) {
            return departmentDAO.findByQrToken(request.token())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid QR code"));
        }
        if ("CODE".equalsIgnoreCase(request.method())) {
            return departmentDAO.findByCheckinCode(request.token())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid check-in code"));
        }
        throw new IllegalArgumentException("Method must be QR or CODE");
    }

    // Call this when staff calls a patient
    @Transactional
    public void callPatient(UUID queueEntryId, UUID roomId) {
        QueueEntry entry = queueEntryDAO.findById(queueEntryId)
                .orElseThrow(() -> new IllegalArgumentException("Queue entry not found"));

        Room room = roomDAO.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (!room.isAvailable()) {
            throw new IllegalStateException("Room is not available");
        }

        if (room.getAssignedStaff() == null) {
            throw new IllegalStateException("No staff assigned to this room");
        }

        if (!room.getDepartment().getId().equals(entry.getDepartment().getId())) {
            throw new IllegalStateException("Room does not belong to the same department as this queue entry");
        }

        Visit visit = entry.getVisit();
        visit.setStatus("CALLED");
        visit.setCalledAt(LocalDateTime.now());
        visit.setRoom(room);
        visit.setAssignedStaff(room.getAssignedStaff());
        visitDAO.save(visit);

        room.setAvailable(false);
        roomDAO.save(room);

        Department department = entry.getDepartment();
        int queueDepth = queueEntryDAO.countActiveInDepartment(department);

        QueueUpdateEvent event = new QueueUpdateEvent(
                department.getId(),
                entry.getPatient().getId(),
                visit.getId(),
                entry.getId(),
                entry.getPatient().getFullName(),
                entry.getPosition(),
                queueDepth,
                entry.getPriorityTier(),
                entry.getAiPriorityScore(),
                0,
                entry.getSymptomSeverity(),
                entry.getPainLevel(),
                entry.getSymptomCategories(),
                entry.getSymptomDuration(),
                visit.getPresentingComplaint(),
                entry.getAdditionalNotes(),
                "CALLED",
                visit.getCheckedInAt(),
                room.getName(),
                room.getAssignedStaff().getFullName(),
                room.getAssignedStaff().getRole(),
                "CALLED"
        );

        publishDepartmentSnapshot(department, "CALLED");
        notificationService.notifyPatient(entry.getPatient().getId(), event);
        notificationService.notifyDepartmentQueue(department.getId(), event);
    }

    // Call this when patient is discharged
    // Call this when patient is discharged
    @Transactional
    public void dischargePatient(UUID queueEntryId) {
        QueueEntry entry = queueEntryDAO.findById(queueEntryId)
                .orElseThrow(() -> new IllegalArgumentException("Queue entry not found"));

        Visit visit = entry.getVisit();
        Department department = entry.getDepartment();
        LocalDateTime now = LocalDateTime.now();

        // Only shift if this entry was still occupying queue space
        if ("CHECKED_IN".equals(visit.getStatus()) || "CALLED".equals(visit.getStatus())) {
            queueEntryDAO.decrementPositionsAfter(department, entry.getPosition());

            // Everyone still in the queue after this point has effectively moved up
            List<QueueEntry> updatedEntries = queueEntryDAO.findByDepartmentOrdered(department);

            for (QueueEntry updatedEntry : updatedEntries) {
                // Skip the entry being discharged if it still appears in the result set
                if (updatedEntry.getId().equals(entry.getId())) {
                    continue;
                }

                updatedEntry.setLastMovedUpAt(now);
                updatedEntry.setLastPositionUpdatedAt(now);
                queueEntryDAO.save(updatedEntry);
            }
        }

        visit.setStatus("DISCHARGED");
        visit.setDischargedAt(now);
        visitDAO.save(visit);

        if (visit.getRoom() != null) {
            Room room = visit.getRoom();
            room.setAvailable(true);
            roomDAO.save(room);
        }

        int queueDepth = queueEntryDAO.countActiveInDepartment(department);

        QueueUpdateEvent event = new QueueUpdateEvent(
                department.getId(),
                entry.getPatient().getId(),
                visit.getId(),
                entry.getId(),
                entry.getPatient().getFullName(),
                entry.getPosition(),
                queueDepth,
                entry.getPriorityTier(),
                entry.getAiPriorityScore(),
                0,
                entry.getSymptomSeverity(),
                entry.getPainLevel(),
                entry.getSymptomCategories(),
                entry.getSymptomDuration(),
                visit.getPresentingComplaint(),
                entry.getAdditionalNotes(),
                "DISCHARGED",
                visit.getCheckedInAt(),
                null,
                null,
                null,
                "DISCHARGED"
        );

        notificationService.notifyPatient(entry.getPatient().getId(), event);
        notificationService.notifyDepartmentQueue(department.getId(), event);
        publishDepartmentSnapshot(department, "DISCHARGED");
    }

    public Optional<CheckInResponse> getQueueStatus(AuthAccount account) {
        Patient patient = account.getPatient();

        return queueEntryDAO.findActiveByPatient(patient)
                .map(entry -> {
                    Visit visit = entry.getVisit();
                    Department department = entry.getDepartment();
                    int queueDepth = queueEntryDAO.countActiveInDepartment(department);
                    int estimatedWait = (entry.getPosition() - 1) * AVG_CONSULTATION_MINUTES;

                    String roomName = visit.getRoom() != null ? visit.getRoom().getName() : null;
                    String assignedStaffName = visit.getAssignedStaff() != null
                            ? visit.getAssignedStaff().getFullName()
                            : null;
                    String assignedStaffRole = visit.getAssignedStaff() != null
                            ? visit.getAssignedStaff().getRole()
                            : null;

                    return new CheckInResponse(
                            visit.getId(),
                            entry.getId(),
                            entry.getPosition(),
                            queueDepth,
                            entry.getPriorityTier(),
                            entry.getAiPriorityScore(),
                            estimatedWait,
                            visit.getFacility().getName(),
                            department.getName(),
                            visit.getStatus(),
                            department.getQrToken(),
                            department.getCheckinCode(),
                            visit.getCheckedInAt(),
                            roomName,
                            assignedStaffName,
                            assignedStaffRole
                    );
                });
    }

    @Transactional
    public void leaveQueue(AuthAccount account) {
        if (account.getPatient() == null) {
            throw new IllegalStateException("No patient profile linked to this account");
        }

        QueueEntry entry = queueEntryDAO.findActiveByPatient(account.getPatient())
                .orElseThrow(() -> new IllegalStateException("No active queue entry found"));

        Patient patient = entry.getPatient(); // use eagerly loaded patient from entry

        Visit visit = entry.getVisit();

        if ("CALLED".equals(visit.getStatus()) || "DISCHARGED".equals(visit.getStatus())) {
            throw new IllegalStateException("Cannot leave queue at this stage");
        }

        Department department = entry.getDepartment();

        queueEntryDAO.decrementPositionsAfter(department, entry.getPosition());

        visit.setStatus("LEFT_QUEUE");
        visit.setDischargedAt(LocalDateTime.now());
        visitDAO.save(visit);

        int queueDepth = queueEntryDAO.countActiveInDepartment(department);

        QueueUpdateEvent leftEvent = new QueueUpdateEvent(
                department.getId(),
                patient.getId(),
                visit.getId(),
                entry.getId(),
                patient.getFullName(),
                entry.getPosition(),
                queueDepth,
                entry.getPriorityTier(),
                entry.getAiPriorityScore(),
                0,
                entry.getSymptomSeverity(),
                entry.getPainLevel(),
                entry.getSymptomCategories(),
                entry.getSymptomDuration(),
                visit.getPresentingComplaint(),
                entry.getAdditionalNotes(),
                "LEFT_QUEUE",
                visit.getCheckedInAt(),
                null,
                null,
                null,
                "LEFT_QUEUE"
        );

        notificationService.notifyPatient(patient.getId(), leftEvent);

        List<QueueEntry> updatedEntries = queueEntryDAO.findByDepartmentOrdered(department);

        LocalDateTime now = LocalDateTime.now();

        for (QueueEntry updatedEntry : updatedEntries) {
            Integer oldPosition = updatedEntry.getPosition();

            updatedEntry.setLastMovedUpAt(now);
            updatedEntry.setLastPositionUpdatedAt(now);
            queueEntryDAO.save(updatedEntry);

            Visit updatedVisit = updatedEntry.getVisit();
            int estimatedWait = (updatedEntry.getPosition() - 1) * AVG_CONSULTATION_MINUTES;

            QueueUpdateEvent bumpedEvent = new QueueUpdateEvent(
                    department.getId(),
                    updatedEntry.getPatient().getId(),
                    updatedVisit.getId(),
                    updatedEntry.getId(),
                    updatedEntry.getPatient().getFullName(),
                    updatedEntry.getPosition(),
                    queueDepth,
                    updatedEntry.getPriorityTier(),
                    updatedEntry.getAiPriorityScore(),
                    estimatedWait,
                    updatedEntry.getSymptomSeverity(),
                    updatedEntry.getPainLevel(),
                    updatedEntry.getSymptomCategories(),
                    updatedEntry.getSymptomDuration(),
                    updatedVisit.getPresentingComplaint(),
                    updatedEntry.getAdditionalNotes(),
                    updatedVisit.getStatus(),
                    updatedVisit.getCheckedInAt(),
                    null,
                    null,
                    null,
                    "QUEUE_UPDATED"
            );

            notificationService.notifyPatient(updatedEntry.getPatient().getId(), bumpedEvent);
        }
        publishDepartmentSnapshot(department, "LEFT_QUEUE");
        notificationService.notifyDepartmentQueue(department.getId(), leftEvent);
    }

    @Transactional
    public void callPatient(UUID queueEntryId, UUID staffId, UUID roomId) {
        QueueEntry entry = queueEntryDAO.findById(queueEntryId)
                .orElseThrow(() -> new IllegalArgumentException("Queue entry not found"));

        Room room = roomDAO.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (!room.isAvailable()) {
            throw new IllegalStateException("Room is not available");
        }

        Staff staff = staffDAO.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found"));

        Visit visit = entry.getVisit();
        visit.setStatus("CALLED");
        visit.setCalledAt(LocalDateTime.now());
        visit.setRoom(room);
        visit.setAssignedStaff(staff);
        visitDAO.save(visit);

        // Mark room as occupied
        room.setAvailable(false);
        roomDAO.save(room);

        Department department = entry.getDepartment();
        int queueDepth = queueEntryDAO.countActiveInDepartment(department);

        QueueUpdateEvent event = new QueueUpdateEvent(
                department.getId(),
                entry.getPatient().getId(),
                visit.getId(),
                entry.getId(),
                entry.getPatient().getFullName(),
                entry.getPosition(),
                queueDepth,
                entry.getPriorityTier(),
                entry.getAiPriorityScore(),
                0,
                entry.getSymptomSeverity(),
                entry.getPainLevel(),
                entry.getSymptomCategories(),
                entry.getSymptomDuration(),
                visit.getPresentingComplaint(),
                entry.getAdditionalNotes(),
                "CALLED",
                visit.getCheckedInAt(),
                null,
                null,
                null,
                "CALLED"
        );

        notificationService.notifyPatient(entry.getPatient().getId(), event);
        notificationService.notifyDepartmentQueue(department.getId(), event);
    }

    @Transactional(readOnly = true)
    public List<QueueEntryViewResponse> getDepartmentQueue(UUID departmentId) {
        Department department = departmentDAO.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));

        return queueEntryDAO.findByDepartmentOrdered(department)
                .stream()
                .map(entry -> {
                    Patient patient = entry.getPatient();
                    Visit visit = entry.getVisit();

                    return new QueueEntryViewResponse(
                            entry.getId(),
                            visit.getId(),
                            patient.getId(),
                            patient.getFullName(),
                            Period.between(patient.getDateOfBirth(), LocalDate.now()).getYears(),
                            department.getName(),
                            entry.getPosition(),
                            entry.getAiPriorityScore(),
                            entry.getPriorityTier(),
                            entry.getSymptomSeverity(),
                            entry.getPainLevel(),
                            entry.getSymptomCategories(),
                            entry.getSymptomDuration(),
                            visit.getPresentingComplaint(),
                            entry.getAdditionalNotes(),
                            visit.getStatus(),
                            visit.getCheckedInAt()
                    );
                })
                .toList();
    }

    @Transactional
    public void swapPositions(UUID queueEntryIdA, UUID queueEntryIdB, UUID staffId) {
        QueueEntry entryA = queueEntryDAO.findById(queueEntryIdA)
                .orElseThrow(() -> new IllegalArgumentException("Queue entry not found: " + queueEntryIdA));
        QueueEntry entryB = queueEntryDAO.findById(queueEntryIdB)
                .orElseThrow(() -> new IllegalArgumentException("Queue entry not found: " + queueEntryIdB));

        if (!entryA.getDepartment().getId().equals(entryB.getDepartment().getId())) {
            throw new IllegalStateException("Cannot swap patients from different departments");
        }

        Staff staff = staffDAO.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));

        int posA = entryA.getPosition();
        int posB = entryB.getPosition();
        LocalDateTime now = LocalDateTime.now();

        entryA.setPosition(posB);
        entryB.setPosition(posA);

        entryA.setLastPositionUpdatedAt(now);
        entryB.setLastPositionUpdatedAt(now);

        // Mark moved-up timestamp only if the new position is better (smaller number)
        if (posB < posA) {
            entryA.setLastMovedUpAt(now);
        }
        if (posA < posB) {
            entryB.setLastMovedUpAt(now);
        }

        queueEntryDAO.save(entryA);
        queueEntryDAO.save(entryB);

        // Record overrides for audit trail
        QueueOverride overrideA = QueueOverride.builder()
                .queueEntry(entryA)
                .staff(staff)
                .originalPosition(posA)
                .newPosition(posB)
                .justification("Manual swap")
                .overriddenAt(now)
                .build();

        QueueOverride overrideB = QueueOverride.builder()
                .queueEntry(entryB)
                .staff(staff)
                .originalPosition(posB)
                .newPosition(posA)
                .justification("Manual swap")
                .overriddenAt(now)
                .build();

        queueOverrideDAO.save(overrideA);
        queueOverrideDAO.save(overrideB);

        Department department = entryA.getDepartment();
        int queueDepth = queueEntryDAO.countActiveInDepartment(department);

        // Notify both patients of their new position
        for (QueueEntry entry : List.of(entryA, entryB)) {
            Visit visit = entry.getVisit();
            int estimatedWait = (entry.getPosition() - 1) * AVG_CONSULTATION_MINUTES;

            QueueUpdateEvent event = new QueueUpdateEvent(
                    department.getId(),
                    entry.getPatient().getId(),
                    visit.getId(),
                    entry.getId(),
                    entry.getPatient().getFullName(),
                    entry.getPosition(),
                    queueDepth,
                    entry.getPriorityTier(),
                    entry.getAiPriorityScore(),
                    estimatedWait,
                    entry.getSymptomSeverity(),
                    entry.getPainLevel(),
                    entry.getSymptomCategories(),
                    entry.getSymptomDuration(),
                    visit.getPresentingComplaint(),
                    entry.getAdditionalNotes(),
                    visit.getStatus(),
                    visit.getCheckedInAt(),
                    null,
                    null,
                    null,
                    "POSITION_UPDATED"
            );

            notificationService.notifyPatient(entry.getPatient().getId(), event);
        }

        // Notify department board
        notificationService.notifyDepartmentQueue(department.getId(), new QueueUpdateEvent(
                department.getId(), null, null, null, null,
                0, queueDepth, null, 0, 0,
                null, null, null, null, null, null,
                null, null, null,
                null,
                null, "QUEUE_UPDATED"
        ));

        publishDepartmentSnapshot(department, "POSITION_UPDATED");
    }

    private void publishDepartmentSnapshot(Department department, String eventType) {

        LocalDateTime now = LocalDateTime.now();

        List<QueueEntry> entries = queueEntryDAO.findByDepartmentOrdered(department);

        // ─────────────────────────────────────────
        // Available rooms
        // ─────────────────────────────────────────
        List<AvailableRoomSnapshot> availableRooms = roomDAO.findByDepartment(department)
                .stream()
                .map(room -> new AvailableRoomSnapshot(
                        room.getId(),
                        room.getName(),
                        room.getDescription(),
                        room.isAvailable(),
                        room.getAssignedStaff() != null ? room.getAssignedStaff().getId() : null,
                        room.getAssignedStaff() != null ? room.getAssignedStaff().getFullName() : null
                ))
                .toList();

        // ─────────────────────────────────────────
        // Available staff
        // ─────────────────────────────────────────
        List<AvailableStaffSnapshot> availableStaff = staffDAO.findByDepartment(department)
                .stream()
                .filter(staff -> Boolean.TRUE.equals(staff.getIsActive()))
                .map(staff -> new AvailableStaffSnapshot(
                        staff.getId(),
                        staff.getFullName(),
                        staff.getRole(),
                        staff.getSpecialty(),
                        Boolean.TRUE.equals(staff.getIsOnDuty()),
                        Boolean.TRUE.equals(staff.getIsActive())
                ))
                .toList();

        // ─────────────────────────────────────────
        // Patient queue snapshots
        // ─────────────────────────────────────────
        List<QueuePatientSnapshot> snapshots = entries.stream()
                .map(entry -> {

                    Visit visit = entry.getVisit();

                    int estimatedWait = Math.max(
                            0,
                            (entry.getPosition() - 1) * AVG_CONSULTATION_MINUTES
                    );

                    int timeInQueueMinutes = visit.getCheckedInAt() != null
                            ? (int) java.time.Duration
                                    .between(visit.getCheckedInAt(), now)
                                    .toMinutes()
                            : 0;

                    String roomName = visit.getRoom() != null
                            ? visit.getRoom().getName()
                            : null;

                    String assignedStaffName = visit.getAssignedStaff() != null
                            ? visit.getAssignedStaff().getFullName()
                            : null;

                    String assignedStaffRole = visit.getAssignedStaff() != null
                            ? visit.getAssignedStaff().getRole()
                            : null;

                    return new QueuePatientSnapshot(
                            entry.getId(),
                            visit.getId(),
                            entry.getPatient().getId(),
                            entry.getPatient().getFullName(),
                            entry.getPosition(),
                            entry.getPriorityTier(),
                            entry.getAiPriorityScore(),
                            entry.getSymptomSeverity(),
                            entry.getPainLevel(),
                            entry.getSymptomCategories(),
                            entry.getSymptomDuration(),
                            visit.getPresentingComplaint(),
                            entry.getAdditionalNotes(),
                            visit.getStatus(),
                            visit.getCheckedInAt(),
                            timeInQueueMinutes,
                            estimatedWait,
                            entry.getLastMovedUpAt(),
                            roomName,
                            assignedStaffName,
                            assignedStaffRole
                    );
                })
                .toList();

        // ─────────────────────────────────────────
        // Average wait time
        // ─────────────────────────────────────────
        int averageEstimatedWaitMinutes = snapshots.isEmpty()
                ? 0
                : (int) snapshots.stream()
                        .map(QueuePatientSnapshot::estimatedWaitMinutes)
                        .filter(java.util.Objects::nonNull)
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(0);

        // ─────────────────────────────────────────
        // Build snapshot event
        // ─────────────────────────────────────────
        DepartmentQueueSnapshotEvent snapshot = new DepartmentQueueSnapshotEvent(
                department.getId(),
                department.getFacility().getName(),
                department.getName(),
                eventType,
                now,
                snapshots.size(),
                averageEstimatedWaitMinutes,
                department.getCapacity(),
                availableRooms,
                availableStaff,
                snapshots
        );

        // ─────────────────────────────────────────
        // Publish to Kafka
        // ─────────────────────────────────────────
        queueKafkaPublisher.publishQueueSnapshot(snapshot);
    }

    private String resolvePriorityTier(int score) {
        if (score >= 95) return "RESUSCITATION";
        if (score >= 85) return "EMERGENCY";
        if (score >= 60) return "URGENT";
        if (score >= 40) return "SEMI_URGENT";
        return "NON_URGENT";
    }

    @Transactional
    public void normalizeQueuePositions(UUID departmentId) {
        Department department = departmentDAO.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));

        List<QueueEntry> entries = queueEntryDAO.findByDepartmentOrdered(department);

        int position = 1;
        LocalDateTime now = LocalDateTime.now();

        for (QueueEntry entry : entries) {
            int oldPosition = entry.getPosition();
            int newPosition = position++;

            if (oldPosition != newPosition) {
                entry.setPosition(newPosition);
                entry.setLastPositionUpdatedAt(now);

                // Only mark move-up if the patient improved in queue
                if (newPosition < oldPosition) {
                    entry.setLastMovedUpAt(now);
                }

                queueEntryDAO.save(entry);
            }
        }

        publishDepartmentSnapshot(department, "QUEUE_NORMALIZED");
    }


}
