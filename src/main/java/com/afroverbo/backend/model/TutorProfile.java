package com.afroverbo.backend.model;

import jakarta.persistence.*;
import lombok.*;

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

    // ✅ New fields
    @Column
    private String profilePicture; // URL to profile picture

    @Column
    private String qualifications; // e.g. "BA Linguistics - UoN, 10 years experience"

    @Column
    private String level; // BEGINNER, INTERMEDIATE, EXPERT

    @Column
    private String experience; // e.g. "10 years teaching Luo"

    @Column(columnDefinition = "TEXT")
    private String availableTimes; // e.g. "Mon-Fri 8am-5pm, Sat 9am-12pm"

    // ✅ Auto calculate platform fee and tutor earnings
    public void setHourlyRate(Double hourlyRate) {
        this.hourlyRate = hourlyRate;
        this.platformFee = hourlyRate * 0.20;
        this.tutorEarnings = hourlyRate * 0.80;
    }
}