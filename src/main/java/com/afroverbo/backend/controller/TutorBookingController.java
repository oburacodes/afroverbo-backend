package com.afroverbo.backend.controller;

import com.afroverbo.backend.model.TutorBooking;
import com.afroverbo.backend.model.TutorProfile;
import com.afroverbo.backend.model.User;
import com.afroverbo.backend.repository.TutorBookingRepository;
import com.afroverbo.backend.repository.TutorProfileRepository;
import com.afroverbo.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TutorBookingController {

    private final TutorBookingRepository tutorBookingRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final UserRepository userRepository;

    // ✅ GET all bookings for a student
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<TutorBooking>> getStudentBookings(@PathVariable Long studentId) {
        return ResponseEntity.ok(tutorBookingRepository.findByStudentId(studentId));
    }

    // ✅ GET all bookings for a tutor
    @GetMapping("/tutor/{tutorId}")
    public ResponseEntity<List<TutorBooking>> getTutorBookings(@PathVariable Long tutorId) {
        return ResponseEntity.ok(tutorBookingRepository.findByTutorId(tutorId));
    }

    // ✅ POST student books a tutor
    @PostMapping
    public ResponseEntity<?> createBooking(@RequestParam Long studentId,
                                           @RequestParam Long tutorId,
                                           @RequestParam String sessionDate,
                                           @RequestParam(required = false) String notes) {

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        User tutor = userRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor not found"));

        TutorProfile tutorProfile = tutorProfileRepository.findByUserId(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor profile not found"));

        if (!tutorProfile.isAvailable()) {
            return ResponseEntity.badRequest().body("Tutor is not available");
        }

        Double amountPaid = tutorProfile.getHourlyRate();
        Double platformCut = tutorProfile.getPlatformFee();
        Double tutorCut = tutorProfile.getTutorEarnings();

        TutorBooking booking = new TutorBooking();
        booking.setStudent(student);
        booking.setTutor(tutor);
        booking.setStatus("PENDING");
        booking.setAmountPaid(amountPaid);
        booking.setPlatformCut(platformCut);
        booking.setTutorCut(tutorCut);
        booking.setBookedAt(LocalDateTime.now());
        booking.setSessionDate(LocalDateTime.parse(sessionDate));
        booking.setNotes(notes);

        return ResponseEntity.ok(tutorBookingRepository.save(booking));
    }
    // ✅ PUT simulate payment
@PutMapping("/{id}/pay")
public ResponseEntity<?> payForBooking(@PathVariable Long id) {
    TutorBooking booking = tutorBookingRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

    if (!booking.getStatus().equals("CONFIRMED")) {
        return ResponseEntity.badRequest().body("Booking must be confirmed before payment");
    }

    booking.setStatus("PAID");
    return ResponseEntity.ok(tutorBookingRepository.save(booking));
}

    // ✅ PUT update booking status with optional tutor reply
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateBookingStatus(@PathVariable Long id,
                                                  @RequestParam String status,
                                                  @RequestParam(required = false) String tutorReply) {

        TutorBooking booking = tutorBookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        booking.setStatus(status);

        // ✅ Save tutor reply if provided
        if (tutorReply != null && !tutorReply.isEmpty()) {
            booking.setTutorReply(tutorReply);
        }

        return ResponseEntity.ok(tutorBookingRepository.save(booking));
    }

    // ✅ DELETE cancel a booking
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelBooking(@PathVariable Long id) {
        TutorBooking booking = tutorBookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        booking.setStatus("CANCELLED");
        return ResponseEntity.ok(tutorBookingRepository.save(booking));
    }
}