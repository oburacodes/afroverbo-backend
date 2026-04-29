package com.afroverbo.backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tutor_availability_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TutorAvailabilitySlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_profile_id", nullable = false)
    @JsonBackReference
    private TutorProfile tutorProfile;

    // Full start datetime e.g. 2026-04-21T09:00
    @Column(nullable = false)
    private LocalDateTime startDateTime;

    // Full end datetime e.g. 2026-04-21T17:00
    @Column(nullable = false)
    private LocalDateTime endDateTime;

    // Optional label e.g. "Morning session", "Afternoon"
    @Column
    private String label;

    @Column(nullable = false)
    private boolean active = true;
}