package com.juno.healthapp.controller;


import com.juno.healthapp.dto.CheckInRequest;
import com.juno.healthapp.dto.CheckInResponse;
import com.juno.healthapp.dto.QueueEntryViewResponse;
import com.juno.healthapp.entity.AuthAccount;
import com.juno.healthapp.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    @PostMapping("/checkin")
    public ResponseEntity<CheckInResponse> checkIn(
            @AuthenticationPrincipal AuthAccount account,
            @RequestBody CheckInRequest request) {
        return ResponseEntity.ok(queueService.checkIn(account, request));
    }

    @GetMapping("/status")
    public ResponseEntity<CheckInResponse> getStatus(
            @AuthenticationPrincipal AuthAccount account) {
        return queueService.getQueueStatus(account)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/call/{queueEntryId}")
    public ResponseEntity<Void> callPatient(
            @PathVariable UUID queueEntryId,
            @RequestParam UUID roomId,
            @AuthenticationPrincipal AuthAccount account) {
        queueService.callPatient(queueEntryId, roomId);
        return ResponseEntity.ok().build();
    }

    // Staff discharges a patient
    @PostMapping("/discharge/{queueEntryId}")
    public ResponseEntity<Void> dischargePatient(
            @PathVariable UUID queueEntryId) {
        queueService.dischargePatient(queueEntryId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/leave")
    public ResponseEntity<Void> leaveQueue(
            @AuthenticationPrincipal AuthAccount account) {
        queueService.leaveQueue(account);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<QueueEntryViewResponse>> getDepartmentQueue(
            @PathVariable UUID departmentId) {
        return ResponseEntity.ok(queueService.getDepartmentQueue(departmentId));
    }

    @PostMapping("/swap")
    public ResponseEntity<Void> swapPositions(
            @RequestParam UUID queueEntryIdA,
            @RequestParam UUID queueEntryIdB,
            @AuthenticationPrincipal AuthAccount account) {
        queueService.swapPositions(queueEntryIdA, queueEntryIdB, account.getStaff().getId());
        return ResponseEntity.ok().build();
    }

}