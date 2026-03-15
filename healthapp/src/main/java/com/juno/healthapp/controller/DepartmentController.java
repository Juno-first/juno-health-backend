package com.juno.healthapp.controller;


import com.juno.healthapp.dto.*;
import com.juno.healthapp.service.DepartmentService;
import com.juno.healthapp.service.QueueService;
import com.juno.healthapp.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;
    private final QueueService queueService;
    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<DepartmentResponse> create(
            @RequestBody CreateDepartmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(departmentService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(departmentService.getById(id));
    }

    @GetMapping("/facility/{facilityId}")
    public ResponseEntity<List<DepartmentResponse>> getByFacility(
            @PathVariable UUID facilityId) {
        return ResponseEntity.ok(departmentService.getByFacility(facilityId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DepartmentResponse> update(
            @PathVariable UUID id,
            @RequestBody UpdateDepartmentRequest request) {
        return ResponseEntity.ok(departmentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{departmentId}/queue")
    public ResponseEntity<List<QueueEntryViewResponse>> getDepartmentQueue(
            @PathVariable UUID departmentId) {
        return ResponseEntity.ok(queueService.getDepartmentQueue(departmentId));
    }

    @GetMapping("/lookup")
    public ResponseEntity<DepartmentLookupResponse> lookup(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String qrToken) {
        return ResponseEntity.ok(departmentService.lookup(code, qrToken));
    }

    @PostMapping("/{departmentId}/rooms")
    public ResponseEntity<RoomResponse> createRoom(
            @PathVariable UUID departmentId,
            @RequestBody CreateRoomRequest request) {
        return ResponseEntity.status(201).body(roomService.createRoom(departmentId, request));
    }

    @GetMapping("/{departmentId}/rooms")
    public ResponseEntity<List<RoomResponse>> getRooms(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(roomService.getRooms(departmentId));
    }

    @GetMapping("/{departmentId}/rooms/available")
    public ResponseEntity<List<RoomResponse>> getAvailableRooms(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(roomService.getAvailableRooms(departmentId));
    }

    @PatchMapping("/rooms/{roomId}/assign-staff")
    public ResponseEntity<RoomResponse> assignStaff(
            @PathVariable UUID roomId,
            @RequestParam UUID staffId) {
        return ResponseEntity.ok(roomService.assignStaff(roomId, staffId));
    }

    @PatchMapping("/rooms/{roomId}/unassign-staff")
    public ResponseEntity<RoomResponse> unassignStaff(@PathVariable UUID roomId) {
        return ResponseEntity.ok(roomService.unassignStaff(roomId));
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deactivateRoom(@PathVariable UUID roomId) {
        roomService.deactivateRoom(roomId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{departmentId}/staff")
    public ResponseEntity<List<StaffResponse>> getStaffByDepartment(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(roomService.getStaffByDepartment(departmentId));
    }

}
