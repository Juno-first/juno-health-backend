package com.juno.healthapp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 30)
    private String gender;

    @Column(name = "parish_of_residence", length = 50)
    private String parishOfResidence;

    @Column(name = "blood_type", length = 5)
    private String bloodType;

    @Column(name = "blood_type_unknown")
    @Builder.Default
    private Boolean bloodTypeUnknown = false;

    @Column(name = "nhf_number", unique = true)
    private String nhfNumber;

    @Column(name = "nids_number", unique = true)
    private String nidsNumber;

    @Column(name = "nids_verified")
    @Builder.Default
    private Boolean nidsVerified = false;

    @Column(name = "language_preference", length = 10)
    private String languagePreference;

    @Column(name = "qr_token", unique = true)
    private String qrToken;

    @Column(name = "qr_token_expires_at")
    private LocalDateTime qrTokenExpiresAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    public String getFullName() {
        return middleName != null
                ? firstName + " " + middleName + " " + lastName
                : firstName + " " + lastName;
    }
}
