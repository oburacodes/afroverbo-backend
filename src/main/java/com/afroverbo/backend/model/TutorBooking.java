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

    // TutorBooking.java

@Enumerated(EnumType.STRING) // This is the "magic" line that prevents the bytea error
@Column(nullable = false)
private BookingStatus status;

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
    private LocalDateTime sessionEndDate;

    @Column
    private LocalDateTime bookedAt;

    // ✅ TO THIS (for safe migration):
    @Column(name = "payment_status", nullable = true)
    private String paymentStatus = "UNPAID";

    @Column
    private String paymentReference;

    @Column
    private String mpesaReceiptNumber;

    @Column
    private String merchantRequestId;

    @Column
    private String checkoutRequestId;

    @Column
    private String phoneNumber;

    @Column
    private LocalDateTime paidAt;
}
