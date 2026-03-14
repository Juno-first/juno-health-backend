package com.juno.healthapp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "facilities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "facility_type", length = 30)
    private String facilityType;

    @Column(name = "address")
    private String address;

    @Column(name = "parish", length = 50)
    private String parish;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "phone", length = 20)
    private String phone;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "opening_hours", columnDefinition = "jsonb")
    private Object openingHours;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "avg_wait_minutes")
    @Builder.Default
    private Integer avgWaitMinutes = 0;

    @Column(name = "nhf_accepted", nullable = false)
    @Builder.Default
    private Boolean nhfAccepted = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "specialties_available", columnDefinition = "jsonb")
    private Object specialtiesAvailable;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "departments", columnDefinition = "jsonb")
    private Object departments;

    @Column(name = "qr_token", unique = true)
    private String qrToken;

    @Column(name = "checkin_code", unique = true, length = 10)
    private String checkinCode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

}
