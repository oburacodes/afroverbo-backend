package com.afroverbo.backend.controller;

import com.afroverbo.backend.model.*;
import com.afroverbo.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user-progress")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserProgressController {

    private final UserProgressRepository userProgressRepository;
    private final UserRepository userRepository;
    private final LessonContentRepository lessonContentRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserLanguageProfileRepository languageProfileRepository;

    // ✅ Get progress for a user in their ACTIVE language
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserProgress>> getUserProgress(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getActiveLanguage() == null) {
            return ResponseEntity.ok(List.of());
        }

        UserLanguageProfile profile = languageProfileRepository
                .findByUserIdAndLanguageId(userId, user.getActiveLanguage().getId())
                .orElseThrow(() -> new RuntimeException("Language profile not found"));

        return ResponseEntity.ok(userProgressRepository.findByLanguageProfileId(profile.getId()));
    }

    // ✅ Get progress summary for active language
    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<?> getProgressSummary(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getActiveLanguage() == null) {
            return ResponseEntity.ok(Map.of("message", "No active language set"));
        }

        UserLanguageProfile profile = languageProfileRepository
                .findByUserIdAndLanguageId(userId, user.getActiveLanguage().getId())
                .orElseThrow(() -> new RuntimeException("Language profile not found"));

        List<UserProgress> allProgress = userProgressRepository
                .findByLanguageProfileId(profile.getId());
        long completed = allProgress.stream().filter(UserProgress::isCompleted).count();
        long total = allProgress.size();
        int percentage = total > 0 ? (int) (completed * 100 / total) : 0;

        List<UserBadge> badges = userBadgeRepository.findByLanguageProfileId(profile.getId());

        Map<String, Object> summary = new HashMap<>();
        summary.put("language", user.getActiveLanguage().getName());
        summary.put("completedLessons", completed);
        summary.put("totalLessons", total);
        summary.put("progressPercentage", percentage);
        summary.put("badges", badges);

        return ResponseEntity.ok(summary);
    }

    // ✅ Mark lesson as started or completed — scoped to active language
    @PostMapping("/user/{userId}/lesson/{lessonId}")
    public ResponseEntity<?> updateProgress(
            @PathVariable Long userId,
            @PathVariable Long lessonId,
            @RequestParam boolean completed,
            @RequestParam(required = false) Integer quizScore) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getActiveLanguage() == null) {
            return ResponseEntity.badRequest().body("No active language set");
        }

        UserLanguageProfile profile = languageProfileRepository
                .findByUserIdAndLanguageId(userId, user.getActiveLanguage().getId())
                .orElseThrow(() -> new RuntimeException("Language profile not found"));

        LessonContent lesson = lessonContentRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        Optional<UserProgress> existingProgress = userProgressRepository
                .findByLanguageProfileIdAndLessonId(profile.getId(), lessonId);

        UserProgress progress = existingProgress.orElse(new UserProgress());
        progress.setUser(user);
        progress.setLanguageProfile(profile);
        progress.setLesson(lesson);
        progress.setCompleted(completed);
        progress.setAttempts(progress.getAttempts() == null ? 1 : progress.getAttempts() + 1);
        progress.setLastAttemptAt(LocalDateTime.now());

        if (quizScore != null) progress.setQuizScore(quizScore);

        if (completed) {
            progress.setCompletedAt(LocalDateTime.now());
            awardBadges(userId, user, profile);
        }

        return ResponseEntity.ok(userProgressRepository.save(progress));
    }

    private void awardBadges(Long userId, User user, UserLanguageProfile profile) {
        long completedCount = userProgressRepository
                .countByLanguageProfileIdAndCompleted(profile.getId(), true);

        if (completedCount == 1 && !userBadgeRepository
                .existsByLanguageProfileIdAndBadgeName(profile.getId(), "FIRST_LESSON")) {
            saveBadge(user, profile, "FIRST_LESSON", "🎯", "Completed your first lesson!");
        }
        if (completedCount == 5 && !userBadgeRepository
                .existsByLanguageProfileIdAndBadgeName(profile.getId(), "FIVE_LESSONS")) {
            saveBadge(user, profile, "FIVE_LESSONS", "⭐", "Completed 5 lessons!");
        }
        if (completedCount == 10 && !userBadgeRepository
                .existsByLanguageProfileIdAndBadgeName(profile.getId(), "TEN_LESSONS")) {
            saveBadge(user, profile, "TEN_LESSONS", "🏆", "Completed 10 lessons!");
        }
    }

    private void saveBadge(User user, UserLanguageProfile profile,
                           String badgeName, String icon, String description) {
        UserBadge badge = new UserBadge();
        badge.setUser(user);
        badge.setLanguageProfile(profile);
        badge.setBadgeName(badgeName);
        badge.setBadgeIcon(icon);
        badge.setDescription(description);
        badge.setEarnedAt(LocalDateTime.now());
        userBadgeRepository.save(badge);
    }
}