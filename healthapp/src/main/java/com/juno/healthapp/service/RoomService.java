package com.juno.healthapp.service;

import com.juno.healthapp.dao.DepartmentDAO;
import com.juno.healthapp.dao.RoomDAO;
import com.juno.healthapp.dao.StaffDAO;
import com.juno.healthapp.dto.CreateRoomRequest;
import com.juno.healthapp.dto.RoomResponse;
import com.juno.healthapp.dto.StaffResponse;
import com.juno.healthapp.entity.Department;
import com.juno.healthapp.entity.Room;
import com.juno.healthapp.entity.Staff;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomDAO roomDAO;
    private final DepartmentDAO departmentDAO;
    private final StaffDAO staffDAO;

    @Transactional
    public RoomResponse createRoom(UUID departmentId, CreateRoomRequest request) {
        Department department = departmentDAO.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));

        Room room = Room.builder()
                .department(department)
                .name(request.name())
                .description(request.description())
                .isAvailable(true)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(roomDAO.save(room));
    }

    public List<RoomResponse> getRooms(UUID departmentId) {
        Department department = departmentDAO.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));
        return roomDAO.findByDepartment(department).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<RoomResponse> getAvailableRooms(UUID departmentId) {
        Department department = departmentDAO.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));
        return roomDAO.findAvailableByDepartment(department).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RoomResponse assignStaff(UUID roomId, UUID staffId) {
        Room room = roomDAO.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        Staff staff = staffDAO.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found"));

        room.setAssignedStaff(staff);
        return toResponse(roomDAO.save(room));
    }

    @Transactional
    public RoomResponse unassignStaff(UUID roomId) {
        Room room = roomDAO.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        room.setAssignedStaff(null);
        return toResponse(roomDAO.save(room));
    }

    @Transactional
    public void deactivateRoom(UUID roomId) {
        Room room = roomDAO.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        room.setActive(false);
        roomDAO.save(room);
    }

    public List<StaffResponse> getStaffByDepartment(UUID departmentId) {
        Department department = departmentDAO.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));
        return staffDAO.findByDepartment(department).stream()
                .map(s -> new StaffResponse(
                        s.getId(),
                        s.getFullName(),
                        s.getRole(),
                        s.getSpecialty(),
                        s.getIsOnDuty()
                ))
                .toList();
    }

    private RoomResponse toResponse(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getDescription(),
                room.isAvailable(),
                room.getAssignedStaff() != null ? room.getAssignedStaff().getId() : null,
                room.getAssignedStaff() != null ? room.getAssignedStaff().getFullName() : null
        );
    }
}