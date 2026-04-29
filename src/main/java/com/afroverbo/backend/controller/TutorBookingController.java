package com.afroverbo.backend.controller;

import com.afroverbo.backend.model.BookingStatus;
import com.afroverbo.backend.model.TutorAvailabilitySlot;
import com.afroverbo.backend.model.TutorBooking;
import com.afroverbo.backend.model.TutorProfile;
import com.afroverbo.backend.model.User;
import com.afroverbo.backend.repository.TutorBookingRepository;
import com.afroverbo.backend.repository.TutorProfileRepository;
import com.afroverbo.backend.repository.UserRepository;
import com.afroverbo.backend.service.DarajaService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TutorBookingController {

    private static final Logger log = LoggerFactory.getLogger(TutorBookingController.class);

    private final TutorBookingRepository tutorBookingRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final UserRepository userRepository;
    private final DarajaService darajaService;

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<TutorBooking>> getStudentBookings(
            @PathVariable Long studentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(tutorBookingRepository.searchBookings(
                studentId, null, normalizeStatus(status), normalize(query),
                parseDateTime(from), parseDateTime(to)));
    }

    @GetMapping("/tutor/{tutorId}")
    public ResponseEntity<List<TutorBooking>> getTutorBookings(
            @PathVariable Long tutorId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(tutorBookingRepository.searchBookings(
                null, tutorId, normalizeStatus(status), normalize(query),
                parseDateTime(from), parseDateTime(to)));
    }

    @PostMapping
    public ResponseEntity<?> createBooking(
            @RequestParam Long studentId,
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

        LocalDateTime sessionStart = parseRequiredDateTime(sessionDate);
        LocalDateTime sessionEnd   = sessionStart.plusHours(1);
        List<TutorAvailabilitySlot> slots = tutorProfile.getAvailabilitySlots();

        log.info("=== BOOKING ATTEMPT ===");
        log.info("Session: {} → {}", sessionStart, sessionEnd);
        log.info("Tutor slots: {}", slots.size());
        slots.forEach(s -> log.info("  Slot: {} → {} active={}",
                s.getStartDateTime(), s.getEndDateTime(), s.isActive()));

        // Only enforce if tutor has configured slots
        if (!slots.isEmpty()) {
            List<TutorAvailabilitySlot> activeSlots = slots.stream()
                    .filter(TutorAvailabilitySlot::isActive).toList();

            boolean withinSlot = activeSlots.stream().anyMatch(slot ->
                    // session must start at or after slot start
                    !sessionStart.isBefore(slot.getStartDateTime())
                    // session must end at or before slot end
                    && !sessionEnd.isAfter(slot.getEndDateTime())
            );

            log.info("withinSlot={}", withinSlot);

            if (!withinSlot) {
                StringBuilder msg = new StringBuilder(
                        "Selected time is outside the tutor's available schedule.\n\nAvailable slots:\n");
                activeSlots.forEach(s -> msg
                        .append("  • ")
                        .append(s.getStartDateTime().toLocalDate()).append(" ")
                        .append(s.getStartDateTime().toLocalTime()).append(" – ")
                        .append(s.getEndDateTime().toLocalTime())
                        .append(s.getLabel() != null ? " (" + s.getLabel() + ")" : "")
                        .append("\n"));
                return ResponseEntity.badRequest().body(msg.toString().trim());
            }
        }

        if (tutorBookingRepository.existsConflictingBooking(tutorId, sessionStart, sessionEnd)) {
            return ResponseEntity.badRequest()
                    .body("Tutor already has a booking at that time. Please choose another slot.");
        }

        TutorBooking booking = new TutorBooking();
        booking.setStudent(student);
        booking.setTutor(tutor);
        booking.setStatus(BookingStatus.PENDING);
        booking.setPaymentStatus("UNPAID");
        booking.setAmountPaid(tutorProfile.getHourlyRate());
        booking.setPlatformCut(tutorProfile.getPlatformFee());
        booking.setTutorCut(tutorProfile.getTutorEarnings());
        booking.setBookedAt(LocalDateTime.now());
        booking.setSessionDate(sessionStart);
        booking.setSessionEndDate(sessionEnd);
        booking.setNotes(notes);

        return ResponseEntity.ok(tutorBookingRepository.save(booking));
    }

    @PutMapping("/{id}/pay")
    public ResponseEntity<?> payForBooking(@PathVariable Long id,
                                           @RequestParam String phoneNumber) {
        TutorBooking booking = tutorBookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        if ("PAID".equalsIgnoreCase(booking.getPaymentStatus()))
            return ResponseEntity.badRequest().body("Booking is already paid");
        return ResponseEntity.ok(darajaService.initiateStkPush(booking, phoneNumber));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateBookingStatus(@PathVariable Long id,
                                                  @RequestParam String status,
                                                  @RequestParam(required = false) String tutorReply) {
        TutorBooking booking = tutorBookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        BookingStatus parsed = parseBookingStatus(status);
        if (parsed == null) return ResponseEntity.badRequest().body("Invalid status: " + status);
        booking.setStatus(parsed);
        if (BookingStatus.CONFIRMED == parsed && "UNPAID".equalsIgnoreCase(booking.getPaymentStatus()))
            booking.setPaymentStatus("PENDING");
        if (BookingStatus.CANCELLED == parsed && !"PAID".equalsIgnoreCase(booking.getPaymentStatus()))
            booking.setPaymentStatus("CANCELLED");
        if (tutorReply != null && !tutorReply.isEmpty())
            booking.setTutorReply(tutorReply);
        return ResponseEntity.ok(tutorBookingRepository.save(booking));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelBooking(@PathVariable Long id) {
        TutorBooking booking = tutorBookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        booking.setStatus(BookingStatus.CANCELLED);
        if (!"PAID".equalsIgnoreCase(booking.getPaymentStatus()))
            booking.setPaymentStatus("CANCELLED");
        return ResponseEntity.ok(tutorBookingRepository.save(booking));
    }

    @PostMapping(value = "/payments/mpesa/callback", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> handleMpesaCallback(@RequestBody Map<String, Object> payload) {
        darajaService.handleCallback(payload);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tutor/{tutorId}/export")
    public ResponseEntity<String> exportTutorBookings(
            @PathVariable Long tutorId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        List<TutorBooking> bookings = tutorBookingRepository.searchBookings(
                null, tutorId, normalizeStatus(status), normalize(query),
                parseDateTime(from), parseDateTime(to));

        StringBuilder csv = new StringBuilder();
        csv.append("Booking ID,Student,Tutor,Status,Payment Status,Session Date,Amount,Tutor Earnings,Phone,Receipt\n");
        for (TutorBooking b : bookings) {
            csv.append(b.getId()).append(',')
               .append(csvValue(b.getStudent().getUsername())).append(',')
               .append(csvValue(b.getTutor().getUsername())).append(',')
               .append(csvValue(b.getStatus().name())).append(',')
               .append(csvValue(b.getPaymentStatus())).append(',')
               .append(csvValue(String.valueOf(b.getSessionDate()))).append(',')
               .append(b.getAmountPaid() == null ? 0 : b.getAmountPaid()).append(',')
               .append(b.getTutorCut()   == null ? 0 : b.getTutorCut()).append(',')
               .append(csvValue(b.getPhoneNumber())).append(',')
               .append(csvValue(b.getMpesaReceiptNumber())).append('\n');
        }
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=tutor-bookings-" + tutorId + ".csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv.toString());
    }

    private String normalizeStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try { return BookingStatus.valueOf(s.toUpperCase().trim()).name(); }
        catch (IllegalArgumentException e) { return null; }
    }

    private BookingStatus parseBookingStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try { return BookingStatus.valueOf(s.toUpperCase().trim()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private LocalDateTime parseRequiredDateTime(String v) {
        LocalDateTime p = parseDateTime(v);
        if (p == null) throw new RuntimeException("Invalid session date");
        return p;
    }

    private LocalDateTime parseDateTime(String v) {
        if (v == null || v.isBlank()) return null;
        try { return LocalDateTime.parse(v.trim()); }
        catch (DateTimeParseException e) {
            throw new RuntimeException("Invalid date-time format. Use ISO-8601 e.g. 2026-04-21T10:00:00");
        }
    }

    private String normalize(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private String csvValue(String v) {
        if (v == null) return "";
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }
}