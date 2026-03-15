package com.juno.healthapp.service;

import com.juno.healthapp.dao.FacilityDAO;
import com.juno.healthapp.dao.FacilityServiceDAO;
import com.juno.healthapp.dao.VisitDAO;
import com.juno.healthapp.dto.*;
import com.juno.healthapp.entity.Facility;
import com.juno.healthapp.entity.FacilityService;
import com.juno.healthapp.httpclient.GeoapifyRoutingClient;
import com.juno.healthapp.util.FacilityCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FacilitiesService {

    private final FacilityDAO facilityDAO;
    private final FacilityServiceDAO facilityServiceDAO;
    private final FacilityCodeGenerator codeGenerator;
    private final VisitDAO visitDAO;
    private final GeoapifyRoutingClient routingClient;

    private static final int AVG_CONSULTATION_MINUTES = 15;

    @Transactional
    public FacilityResponse create(CreateFacilityRequest request) {
        Facility facility = Facility.builder()
                .name(request.name())
                .description(request.description())
                .facilityType(request.facilityType())
                .address(request.address())
                .parish(request.parish())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .phone(request.phone())
                .nhfAccepted(request.nhfAccepted())
                .openingHours(request.openingHours())
                .createdAt(LocalDateTime.now())
                .checkinCode(codeGenerator.generateCheckinCode())
                .qrToken(codeGenerator.generateQrToken())
                .build();
        facility = facilityDAO.save(facility);
        return toResponse(facility, List.of(), 0);
    }

    public FacilityResponse getById(UUID id) {
        Facility facility = facilityDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found"));
        return toResponse(facility, getServiceResponses(id), calculateAvgWait(id));
    }

    public List<FacilityResponse> getAll() {
        return facilityDAO.findAll()
                .stream()
                .map(f -> toResponse(f, getServiceResponses(f.getId()), calculateAvgWait(f.getId())))
                .collect(Collectors.toList());
    }

    public List<FacilityResponse> getByParish(String parish) {
        return facilityDAO.findByParish(parish)
                .stream()
                .map(f -> toResponse(f, getServiceResponses(f.getId()), calculateAvgWait(f.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public FacilityResponse update(UUID id, UpdateFacilityRequest request) {
        Facility facility = facilityDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found"));

        if (request.name() != null) facility.setName(request.name());
        if (request.description() != null) facility.setDescription(request.description());
        if (request.facilityType() != null) facility.setFacilityType(request.facilityType());
        if (request.address() != null) facility.setAddress(request.address());
        if (request.parish() != null) facility.setParish(request.parish());
        if (request.latitude() != null) facility.setLatitude(request.latitude());
        if (request.longitude() != null) facility.setLongitude(request.longitude());
        if (request.phone() != null) facility.setPhone(request.phone());
        if (request.nhfAccepted() != null) facility.setNhfAccepted(request.nhfAccepted());

        facility = facilityDAO.save(facility);
        return toResponse(facility, getServiceResponses(id), calculateAvgWait(id));
    }

    @Transactional
    public void delete(UUID id) {
        Facility facility = facilityDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found"));
        facilityDAO.delete(facility);
    }

    @Transactional
    public FacilityServiceResponse addService(FacilityServiceRequest request) {
        Facility facility = facilityDAO.findById(request.facilityId())
                .orElseThrow(() -> new IllegalArgumentException("Facility not found"));

        FacilityService service = FacilityService.builder()
                .facility(facility)
                .name(request.name())
                .description(request.description())
                .build();

        service = facilityServiceDAO.save(service);
        return new FacilityServiceResponse(service.getId(), service.getName(), service.getDescription());
    }

    @Transactional
    public void removeService(UUID serviceId) {
        facilityServiceDAO.delete(serviceId);
    }

    public List<FacilityServiceResponse> getServicesForFacility(UUID facilityId) {
        return getServiceResponses(facilityId);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private int calculateAvgWait(UUID facilityId) {
        int activeVisits = visitDAO.countActiveVisitsByFacility(facilityId);
        return activeVisits * AVG_CONSULTATION_MINUTES;
    }

    private List<FacilityServiceResponse> getServiceResponses(UUID facilityId) {
        return facilityServiceDAO.findByFacilityId(facilityId)
                .stream()
                .map(s -> new FacilityServiceResponse(s.getId(), s.getName(), s.getDescription()))
                .collect(Collectors.toList());
    }

    public List<FacilityResponse> getNearestFacilities(
            BigDecimal lat, BigDecimal lon,
            double radiusKm, int limit) {

        List<Facility> facilities = facilityDAO.findNearest(lat, lon, radiusKm, limit);

        return facilities.stream()
                .map(f -> {
                    double distance = haversine(lat, lon, f.getLatitude(), f.getLongitude());

                    List<FacilityServiceResponse> services = facilityServiceDAO
                            .findByFacilityId(f.getId())
                            .stream()
                            .map(s -> new FacilityServiceResponse(s.getId(), s.getName(), s.getDescription()))
                            .toList();

                    RouteInfo route = routingClient.getRoute(lat, lon, f.getLatitude(), f.getLongitude());

                    return toResponse(f, services, calculateAvgWait(f.getId()),
                            Math.round(distance * 10.0) / 10.0, route);
                })
                .toList();
    }

    private double haversine(BigDecimal lat1, BigDecimal lon1,
                             BigDecimal lat2, BigDecimal lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2.subtract(lat1).doubleValue());
        double dLon = Math.toRadians(lon2.subtract(lon1).doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue()))
                * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
    private FacilityResponse toResponse(Facility facility, List<FacilityServiceResponse> services, int avgWait) {
        return new FacilityResponse(
                facility.getId(),
                facility.getName(),
                facility.getDescription(),
                facility.getFacilityType(),
                facility.getAddress(),
                facility.getParish(),
                facility.getLatitude(),
                facility.getLongitude(),
                facility.getPhone(),
                facility.getNhfAccepted(),
                facility.getCheckinCode(),
                facility.getQrToken(),
                facility.getCreatedAt(),
                avgWait,
                services,
                null,
                null
        );
    }

    private FacilityResponse toResponse(Facility facility, List<FacilityServiceResponse> services, int avgWait, Double distanceKm) {
        return new FacilityResponse(
                facility.getId(),
                facility.getName(),
                facility.getDescription(),
                facility.getFacilityType(),
                facility.getAddress(),
                facility.getParish(),
                facility.getLatitude(),
                facility.getLongitude(),
                facility.getPhone(),
                facility.getNhfAccepted(),
                facility.getCheckinCode(),
                facility.getQrToken(),
                facility.getCreatedAt(),
                avgWait,
                services,
                distanceKm,
                null
        );
    }
    private FacilityResponse toResponse(Facility facility, List<FacilityServiceResponse> services,
                                        int avgWait, Double distanceKm, RouteInfo route) {
        return new FacilityResponse(
                facility.getId(),
                facility.getName(),
                facility.getDescription(),
                facility.getFacilityType(),
                facility.getAddress(),
                facility.getParish(),
                facility.getLatitude(),
                facility.getLongitude(),
                facility.getPhone(),
                facility.getNhfAccepted(),
                facility.getCheckinCode(),
                facility.getQrToken(),
                facility.getCreatedAt(),
                avgWait,
                services,
                distanceKm,
                route
        );
    }
}
