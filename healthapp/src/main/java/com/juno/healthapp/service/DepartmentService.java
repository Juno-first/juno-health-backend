package com.juno.healthapp.service;

import com.juno.healthapp.dao.DepartmentDAO;
import com.juno.healthapp.dao.FacilityDAO;
import com.juno.healthapp.dto.*;
import com.juno.healthapp.entity.Department;
import com.juno.healthapp.entity.Facility;
import com.juno.healthapp.util.FacilityCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentDAO departmentDAO;
    private final FacilityDAO facilityDAO;
    private final FacilityCodeGenerator codeGenerator;

    @Transactional
    public DepartmentResponse create(CreateDepartmentRequest request) {
        Facility facility = facilityDAO.findById(request.facilityId())
                .orElseThrow(() -> new IllegalArgumentException("Facility not found"));

        Department department = Department.builder()
                .facility(facility)
                .name(request.name())
                .description(request.description())
                .departmentType(request.departmentType())
                .capacity(request.capacity())
                .qrToken(codeGenerator.generateQrToken())
                .checkinCode(codeGenerator.generateCheckinCode())
                .build();

        department = departmentDAO.save(department);
        return toResponse(department);
    }

    public DepartmentResponse getById(UUID id) {
        Department department = departmentDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));
        return toResponse(department);
    }

    public List<DepartmentResponse> getByFacility(UUID facilityId) {
        Facility facility = facilityDAO.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found"));
        return departmentDAO.findActiveByFacility(facility)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public DepartmentLookupResponse lookup(String code, String qrToken) {
        Department department;

        if (qrToken != null) {
            department = departmentDAO.findByQrToken(qrToken)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid QR token"));
        } else if (code != null) {
            department = departmentDAO.findByCheckinCode(code)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid check-in code"));
        } else {
            throw new IllegalArgumentException("Either code or qrToken must be provided");
        }

        Facility facility = department.getFacility();

        List<FacilityServiceResponse> services = facilityDAO.findActiveByFacility(facility)
                .stream()
                .map(s -> new FacilityServiceResponse(s.getId(), s.getName(), s.getDescription()))
                .toList();

        return new DepartmentLookupResponse(
                facility.getName(),
                facility.getAddress(),
                department.getName(),
                facility.getOpeningHours(),
                services
        );
    }

    @Transactional
    public DepartmentResponse update(UUID id, UpdateDepartmentRequest request) {
        Department department = departmentDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));

        if (request.name() != null) department.setName(request.name());
        if (request.departmentType() != null) department.setDepartmentType(request.departmentType());
        if (request.capacity() != null) department.setCapacity(request.capacity());
        if (request.isActive() != null) department.setIsActive(request.isActive());
        if (request.description() != null) department.setDescription(request.description());

        department = departmentDAO.save(department);
        return toResponse(department);
    }

    @Transactional
    public void delete(UUID id) {
        Department department = departmentDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));
        departmentDAO.delete(department);
    }

    private DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getFacility().getId(),
                department.getFacility().getName(),
                department.getName(),
                department.getDescription(),
                department.getDepartmentType(),
                department.getCapacity(),
                department.getIsActive(),
                department.getQrToken(),
                department.getCheckinCode()
        );
    }
}