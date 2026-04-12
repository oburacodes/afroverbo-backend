package com.afroverbo.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tutor_bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TutorBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tutor_id")
    private User tutor;

    @Column(nullable = false)
    private String status; // PENDING, CONFIRMED, CANCELLED, PAID, COMPLETED

    @Column
    private Double amountPaid;

    @Column
    private Double platformCut;

    @Column
    private Double tutorCut;

    @Column
    private String notes;

    @Column
    private String tutorReply;

    @Column
    private LocalDateTime sessionDate;

    @Column
    private LocalDateTime bookedAt;
}