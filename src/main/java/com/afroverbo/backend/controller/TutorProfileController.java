package com.afroverbo.backend.controller;

import com.afroverbo.backend.model.TutorAvailabilitySlot;
import com.afroverbo.backend.model.TutorProfile;
import com.afroverbo.backend.model.User;
import com.afroverbo.backend.repository.TutorProfileRepository;
import com.afroverbo.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/tutors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TutorProfileController {

    private final TutorProfileRepository tutorProfileRepository;
    private final UserRepository userRepository;

    // ── GET all tutors (with search/filter) ───────────────────────────────────
    @GetMapping
    public ResponseEntity<List<TutorProfile>> getAllTutors(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Double minRate,
            @RequestParam(required = false) Double maxRate) {
        return ResponseEntity.ok(
                tutorProfileRepository.searchTutors(
                        normalize(query), normalize(specialization), available, minRate, maxRate));
    }

    // ── GET by specialization ─────────────────────────────────────────────────
    @GetMapping("/specialization/{language}")
    public ResponseEntity<List<TutorProfile>> getBySpecialization(@PathVariable String language) {
        return ResponseEntity.ok(tutorProfileRepository.findBySpecializationAndAvailable(language, true));
    }

    // ── GET single profile ────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<TutorProfile> getTutorProfile(@PathVariable Long id) {
        return ResponseEntity.ok(tutorProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tutor profile not found")));
    }

    // ── GET by user ID ────────────────────────────────────────────────────────
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getTutorProfileByUserId(@PathVariable Long userId) {
        return tutorProfileRepository.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── GET availability slots ────────────────────────────────────────────────
    @GetMapping("/{id}/availability")
    public ResponseEntity<List<TutorAvailabilitySlot>> getAvailability(@PathVariable Long id) {
        TutorProfile profile = tutorProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tutor profile not found"));
        return ResponseEntity.ok(profile.getAvailabilitySlots());
    }

    // ── POST create profile (admin only) ──────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> createTutorProfile(@RequestBody TutorProfile tutorProfile,
                                                 @RequestParam Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            if (!user.getRole().equals("ADMIN")) {
                return ResponseEntity.badRequest().body("Only admins can create tutor profiles");
            }
            if (tutorProfileRepository.findByUserId(userId).isPresent()) {
                return ResponseEntity.badRequest().body("Tutor profile already exists");
            }
            tutorProfile.setUser(user);
            syncSlots(tutorProfile, tutorProfile.getAvailabilitySlots());
            return ResponseEntity.ok(tutorProfileRepository.save(tutorProfile));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // ── PUT update full profile ───────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTutorProfile(@PathVariable Long id,
                                                 @RequestBody TutorProfile updated) {
        TutorProfile profile = tutorProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tutor profile not found"));

        if (updated.getBio()             != null) profile.setBio(updated.getBio());
        if (updated.getHourlyRate()      != null) profile.setHourlyRate(updated.getHourlyRate());
        if (updated.getSpecialization()  != null) profile.setSpecialization(updated.getSpecialization());
        if (updated.getQualifications()  != null) profile.setQualifications(updated.getQualifications());
        if (updated.getLevel()           != null) profile.setLevel(updated.getLevel());
        if (updated.getExperience()      != null) profile.setExperience(updated.getExperience());
        if (updated.getProfilePicture()  != null) profile.setProfilePicture(updated.getProfilePicture());
        if (updated.getAvailableTimes()  != null) profile.setAvailableTimes(updated.getAvailableTimes());
        if (updated.getPayoutPhoneNumber() != null) profile.setPayoutPhoneNumber(updated.getPayoutPhoneNumber());
        profile.setAvailable(updated.isAvailable());

        if (updated.getAvailabilitySlots() != null) {
            syncSlots(profile, updated.getAvailabilitySlots());
        }

        return ResponseEntity.ok(tutorProfileRepository.save(profile));
    }

    // ── PUT update availability slots only ────────────────────────────────────
    @PutMapping("/{id}/availability")
    public ResponseEntity<?> updateAvailability(@PathVariable Long id,
                                                @RequestBody List<TutorAvailabilitySlot> slots) {
        TutorProfile profile = tutorProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tutor profile not found"));
        syncSlots(profile, slots);
        return ResponseEntity.ok(tutorProfileRepository.save(profile));
    }

    // ── Sync slots helper ─────────────────────────────────────────────────────
    private void syncSlots(TutorProfile profile, List<TutorAvailabilitySlot> incoming) {
        profile.getAvailabilitySlots().clear();
        if (incoming == null || incoming.isEmpty()) return;

        for (TutorAvailabilitySlot s : incoming) {
            if (s.getStartDateTime() == null || s.getEndDateTime() == null) continue;
            if (!s.getEndDateTime().isAfter(s.getStartDateTime())) {
                throw new RuntimeException(
                        "Slot end time must be after start time: " + s.getStartDateTime());
            }
            TutorAvailabilitySlot slot = new TutorAvailabilitySlot();
            slot.setTutorProfile(profile);
            slot.setStartDateTime(s.getStartDateTime());
            slot.setEndDateTime(s.getEndDateTime());
            slot.setLabel(s.getLabel());
            slot.setActive(s.isActive());
            profile.getAvailabilitySlots().add(slot);
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}