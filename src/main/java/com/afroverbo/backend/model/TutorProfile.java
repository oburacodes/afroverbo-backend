package com.afroverbo.backend.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tutor_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TutorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    @Column
    private String bio;

    @Column
    private Double hourlyRate;

    @Column
    private Double platformFee;

    @Column
    private Double tutorEarnings;

    @Column
    private String specialization;

    @Column
    private boolean available;

    @Column
    private String profilePicture;

    @Column
    private String qualifications;

    @Column
    private String level; // BEGINNER, INTERMEDIATE, EXPERT

    @Column
    private String experience;

    @Column(columnDefinition = "TEXT")
    private String availableTimes; // legacy free-text, kept for display

    // M-Pesa number where tutor earnings are sent
    @Column
    private String payoutPhoneNumber;

    @OneToMany(mappedBy = "tutorProfile", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("startDateTime ASC")
    @JsonManagedReference
    private List<TutorAvailabilitySlot> availabilitySlots = new ArrayList<>();

    // Auto-calculate platform fee and tutor earnings when rate is set
    public void setHourlyRate(Double hourlyRate) {
        this.hourlyRate    = hourlyRate;
        this.platformFee   = hourlyRate != null ? hourlyRate * 0.20 : null;
        this.tutorEarnings = hourlyRate != null ? hourlyRate * 0.80 : null;
    }
}