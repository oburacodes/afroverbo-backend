package com.afroverbo.backend.controller;

import com.afroverbo.backend.model.Lesson;
import com.afroverbo.backend.model.LessonProgress;
import com.afroverbo.backend.model.User;
import com.afroverbo.backend.repository.LessonProgressRepository;
import com.afroverbo.backend.repository.LessonRepository;
import com.afroverbo.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LessonProgressController {

    private final LessonProgressRepository lessonProgressRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;

    // ✅ GET all progress for a student
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<LessonProgress>> getUserProgress(@PathVariable Long userId) {
        return ResponseEntity.ok(lessonProgressRepository.findByUserId(userId));
    }

    // ✅ GET completed lessons for a student
    @GetMapping("/user/{userId}/completed")
    public ResponseEntity<List<LessonProgress>> getCompletedLessons(@PathVariable Long userId) {
        return ResponseEntity.ok(lessonProgressRepository.findByUserIdAndCompleted(userId, true));
    }

    // ✅ GET student summary (how many lessons completed etc.)
    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<Map<String, Object>> getUserSummary(@PathVariable Long userId) {
        int completed = lessonProgressRepository.countByUserIdAndCompleted(userId, true);
        int total = lessonProgressRepository.findByUserId(userId).size();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalLessons", total);
        summary.put("completedLessons", completed);
        summary.put("progressPercentage", total > 0 ? (completed * 100 / total) : 0);

        return ResponseEntity.ok(summary);
    }

    // ✅ POST mark a lesson as started or completed
    @PostMapping("/user/{userId}/lesson/{lessonId}")
    public ResponseEntity<?> updateProgress(@PathVariable Long userId,
                                            @PathVariable Long lessonId,
                                            @RequestParam boolean completed,
                                            @RequestParam(defaultValue = "0") int score) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        // check if progress record already exists
        Optional<LessonProgress> existing = lessonProgressRepository
                .findByUserIdAndLessonId(userId, lessonId);

        LessonProgress progress = existing.orElse(new LessonProgress());
        progress.setUser(user);
        progress.setLesson(lesson);
        progress.setCompleted(completed);
        progress.setScore(score);

        if (completed) {
            progress.setCompletedAt(LocalDateTime.now());
        }

        return ResponseEntity.ok(lessonProgressRepository.save(progress));
    }
}