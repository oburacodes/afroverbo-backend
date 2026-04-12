package com.afroverbo.backend.controller;

import com.afroverbo.backend.model.*;
import com.afroverbo.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user-languages")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserLanguageController {

    private final UserRepository userRepository;
    private final LanguageRepository languageRepository;
    private final UserLanguageProfileRepository profileRepository;

    // ✅ Get all languages a user is enrolled in
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserLanguageProfile>> getEnrolledLanguages(@PathVariable Long userId) {
        return ResponseEntity.ok(profileRepository.findByUserId(userId));
    }

    // ✅ Enroll in a new language (or re-enroll)
    @PostMapping("/user/{userId}/enroll/{languageId}")
    public ResponseEntity<?> enrollInLanguage(
            @PathVariable Long userId,
            @PathVariable Long languageId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Language language = languageRepository.findById(languageId)
                .orElseThrow(() -> new RuntimeException("Language not found"));

        // Create profile if it doesn't exist
        if (!profileRepository.existsByUserIdAndLanguageId(userId, languageId)) {
            UserLanguageProfile profile = new UserLanguageProfile();
            profile.setUser(user);
            profile.setLanguage(language);
            profile.setEnrolledAt(LocalDateTime.now());
            profileRepository.save(profile);
        }

        // Switch active language
        user.setActiveLanguage(language);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "message", "Enrolled in " + language.getName(),
            "activeLanguage", language
        ));
    }

    // ✅ Switch active language (must already be enrolled)
    @PutMapping("/user/{userId}/switch/{languageId}")
    public ResponseEntity<?> switchLanguage(
            @PathVariable Long userId,
            @PathVariable Long languageId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Language language = languageRepository.findById(languageId)
                .orElseThrow(() -> new RuntimeException("Language not found"));

        UserLanguageProfile profile = profileRepository
                .findByUserIdAndLanguageId(userId, languageId)
                .orElseThrow(() -> new RuntimeException(
                    "Not enrolled in this language. Enroll first."));

        // Update last active
        profile.setLastActiveAt(LocalDateTime.now());
        profileRepository.save(profile);

        // Switch active language on user
        user.setActiveLanguage(language);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "message", "Switched to " + language.getName(),
            "activeLanguage", language
        ));
    }
}