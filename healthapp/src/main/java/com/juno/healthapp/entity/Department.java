package com.juno.healthapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "department_type", length = 30)
    private String departmentType;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "qr_token", unique = true)
    private String qrToken;

    @Column(name = "checkin_code", unique = true, length = 10)
    private String checkinCode;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}