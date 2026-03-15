package com.juno.healthapp.controller;

import com.juno.healthapp.dto.*;
import com.juno.healthapp.service.FacilitiesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/facilities")
@RequiredArgsConstructor
public class FacilityController {

    private final FacilitiesService facilityService;

    @PostMapping
    public ResponseEntity<FacilityResponse> create(
            @RequestBody CreateFacilityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facilityService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacilityResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(facilityService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<FacilityResponse>> getAll() {
        return ResponseEntity.ok(facilityService.getAll());
    }

    @GetMapping("/parish/{parish}")
    public ResponseEntity<List<FacilityResponse>> getByParish(
            @PathVariable String parish) {
        return ResponseEntity.ok(facilityService.getByParish(parish));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<FacilityResponse> update(
            @PathVariable UUID id,
            @RequestBody UpdateFacilityRequest request) {
        return ResponseEntity.ok(facilityService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        facilityService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/services")
    public ResponseEntity<FacilityServiceResponse> addService(
            @RequestBody FacilityServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facilityService.addService(request));
    }

    @DeleteMapping("/services/{serviceId}")
    public ResponseEntity<Void> removeService(@PathVariable UUID serviceId) {
        facilityService.removeService(serviceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<FacilityResponse>> getNearbyFacilities(
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lon,
            @RequestParam(defaultValue = "10") double radiusKm,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(facilityService.getNearestFacilities(lat, lon, radiusKm, limit));
    }
}