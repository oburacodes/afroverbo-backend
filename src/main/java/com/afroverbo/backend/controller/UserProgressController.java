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
    private final QuizAttemptRepository quizAttemptRepository;
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
        long trackedLessons = allProgress.size();
        long totalLessons = lessonContentRepository.countByModuleCourseLanguageId(user.getActiveLanguage().getId());
        long attemptedLessons = allProgress.stream()
                .filter(progress -> progress.getQuizScore() != null)
                .count();
        int averageScore = attemptedLessons > 0
                ? (int) Math.round(allProgress.stream()
                .filter(progress -> progress.getQuizScore() != null)
                .mapToInt(progress -> progress.getBestQuizScore() != null
                        ? progress.getBestQuizScore()
                        : progress.getQuizScore())
                .average()
                .orElse(0))
                : 0;
        int completionPercentage = totalLessons > 0
                ? (int) Math.round((completed * 100.0) / totalLessons)
                : 0;
        long totalAttempts = allProgress.stream()
                .map(UserProgress::getAttempts)
                .filter(attempts -> attempts != null)
                .mapToLong(Integer::longValue)
                .sum();

        List<UserBadge> badges = userBadgeRepository.findByLanguageProfileId(profile.getId());

        Map<String, Object> summary = new HashMap<>();
        summary.put("language", user.getActiveLanguage().getName());
        summary.put("completedLessons", completed);
        summary.put("totalLessons", totalLessons);
        summary.put("trackedLessons", trackedLessons);
        summary.put("attemptedLessons", attemptedLessons);
        summary.put("progressPercentage", averageScore);
        summary.put("averageScore", averageScore);
        summary.put("completionPercentage", completionPercentage);
        summary.put("totalAttempts", totalAttempts);
        summary.put("badges", badges);

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/user/{userId}/lesson/{lessonId}/attempts")
    public ResponseEntity<List<QuizAttempt>> getLessonAttempts(@PathVariable Long userId,
                                                               @PathVariable Long lessonId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getActiveLanguage() == null) {
            return ResponseEntity.ok(List.of());
        }

        UserLanguageProfile profile = languageProfileRepository
                .findByUserIdAndLanguageId(userId, user.getActiveLanguage().getId())
                .orElseThrow(() -> new RuntimeException("Language profile not found"));

        Optional<UserProgress> progress = userProgressRepository
                .findByLanguageProfileIdAndLessonId(profile.getId(), lessonId);

        if (progress.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        return ResponseEntity.ok(
                quizAttemptRepository.findByUserProgressIdOrderByAttemptNumberAsc(progress.get().getId())
        );
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
        int previousAttempts = progress.getAttempts() == null ? 0 : progress.getAttempts();
        boolean wasCompleted = progress.isCompleted();
        LocalDateTime now = LocalDateTime.now();

        progress.setUser(user);
        progress.setLanguageProfile(profile);
        progress.setLesson(lesson);
        progress.setCompleted(wasCompleted || completed);

        if (quizScore != null) {
            progress.setQuizScore(quizScore);
            progress.setBestQuizScore(progress.getBestQuizScore() == null
                    ? quizScore
                    : Math.max(progress.getBestQuizScore(), quizScore));
            progress.setAttempts(previousAttempts + 1);
            progress.setLastAttemptAt(now);
        } else if (progress.getLastAttemptAt() == null) {
            progress.setLastAttemptAt(now);
        }

        if (progress.isCompleted() && progress.getCompletedAt() == null) {
            progress.setCompletedAt(now);
        }

        UserProgress savedProgress = userProgressRepository.save(progress);

        if (quizScore != null) {
            QuizAttempt attempt = new QuizAttempt();
            attempt.setUserProgress(savedProgress);
            attempt.setAttemptNumber(previousAttempts + 1);
            attempt.setScore(quizScore);
            attempt.setCompleted(savedProgress.isCompleted());
            attempt.setAttemptedAt(now);
            quizAttemptRepository.save(attempt);
        }

        if (!wasCompleted && savedProgress.isCompleted()) {
            awardBadges(user, profile);
        }

        return ResponseEntity.ok(savedProgress);
    }

    private void awardBadges(User user, UserLanguageProfile profile) {
        long completedCount = userProgressRepository
                .countByLanguageProfileIdAndCompleted(profile.getId(), true);
        long totalLessons = lessonContentRepository.countByModuleCourseLanguageId(profile.getLanguage().getId());

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
        if (totalLessons > 0
                && completedCount >= totalLessons
                && !userBadgeRepository.existsByLanguageProfileIdAndBadgeName(profile.getId(), "ALL_LESSONS_COMPLETED")) {
            saveBadge(user, profile, "ALL_LESSONS_COMPLETED", "👑", "Completed all lessons in this language!");
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
