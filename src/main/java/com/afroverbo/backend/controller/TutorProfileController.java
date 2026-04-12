package com.afroverbo.backend.controller;

import com.afroverbo.backend.model.TutorProfile;
import com.afroverbo.backend.model.User;
import com.afroverbo.backend.repository.TutorProfileRepository;
import com.afroverbo.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tutors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TutorProfileController {

    private final TutorProfileRepository tutorProfileRepository;
    private final UserRepository userRepository;

    // ✅ GET all available tutors
    @GetMapping
    public ResponseEntity<List<TutorProfile>> getAllAvailableTutors() {
        return ResponseEntity.ok(tutorProfileRepository.findByAvailable(true));
    }

    // ✅ GET tutors by specialization (language)
    @GetMapping("/specialization/{language}")
    public ResponseEntity<List<TutorProfile>> getTutorsBySpecialization(@PathVariable String language) {
        return ResponseEntity.ok(tutorProfileRepository.findBySpecializationAndAvailable(language, true));
    }

    // ✅ GET single tutor profile
    @GetMapping("/{id}")
    public ResponseEntity<TutorProfile> getTutorProfile(@PathVariable Long id) {
        TutorProfile profile = tutorProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tutor profile not found"));
        return ResponseEntity.ok(profile);
    }

    // ✅ GET tutor profile by user ID
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getTutorProfileByUserId(@PathVariable Long userId) {
    return tutorProfileRepository.findByUserId(userId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
}

    // ✅ POST create tutor profile — admin only
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
                return ResponseEntity.badRequest().body("Tutor profile already exists for this user");
            }

            tutorProfile.setUser(user);
            tutorProfile.setHourlyRate(tutorProfile.getHourlyRate());

            return ResponseEntity.ok(tutorProfileRepository.save(tutorProfile));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }



    // ✅ PUT update tutor profile
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTutorProfile(@PathVariable Long id,
                                                 @RequestBody TutorProfile updatedProfile) {

        TutorProfile profile = tutorProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tutor profile not found"));

        profile.setBio(updatedProfile.getBio());
        profile.setHourlyRate(updatedProfile.getHourlyRate());
        profile.setSpecialization(updatedProfile.getSpecialization());
        profile.setAvailable(updatedProfile.isAvailable());
        profile.setQualifications(updatedProfile.getQualifications());
        profile.setLevel(updatedProfile.getLevel());
        profile.setExperience(updatedProfile.getExperience());
        profile.setProfilePicture(updatedProfile.getProfilePicture());
        profile.setAvailableTimes(updatedProfile.getAvailableTimes());

        return ResponseEntity.ok(tutorProfileRepository.save(profile));
    }
}