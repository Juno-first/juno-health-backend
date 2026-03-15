package com.juno.healthapp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "queue_overrides")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueueOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_entry_id", nullable = false)
    private QueueEntry queueEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Column(name = "original_position", nullable = false)
    private Integer originalPosition;

    @Column(name = "new_position", nullable = false)
    private Integer newPosition;

    @Column(name = "justification", columnDefinition = "TEXT", nullable = false)
    private String justification;

    @CreationTimestamp
    @Column(name = "overridden_at", updatable = false)
    private LocalDateTime overriddenAt;
}