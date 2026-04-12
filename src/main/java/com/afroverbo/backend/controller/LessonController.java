package com.afroverbo.backend.controller;

import com.afroverbo.backend.model.Lesson;
import com.afroverbo.backend.model.User;
import com.afroverbo.backend.repository.LessonRepository;
import com.afroverbo.backend.repository.UserRepository;
import com.afroverbo.backend.repository.LanguageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LessonController {

    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final LanguageRepository languageRepository;

    // ✅ GET all lessons for a specific language
    @GetMapping("/language/{languageId}")
    public ResponseEntity<List<Lesson>> getLessonsByLanguage(@PathVariable Long languageId) {
        return ResponseEntity.ok(lessonRepository.findByLanguageId(languageId));
    }

    // ✅ GET lessons by language and level
    @GetMapping("/language/{languageId}/level/{level}")
    public ResponseEntity<List<Lesson>> getLessonsByLanguageAndLevel(
            @PathVariable Long languageId,
            @PathVariable String level) {
        return ResponseEntity.ok(lessonRepository.findByLanguageIdAndLevel(languageId, level));
    }

    // ✅ GET single lesson by id
    @GetMapping("/{id}")
    public ResponseEntity<Lesson> getLesson(@PathVariable Long id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));
        return ResponseEntity.ok(lesson);
    }

    // ✅ POST create a lesson — tutor/admin only
    @PostMapping
    public ResponseEntity<?> createLesson(@RequestBody Lesson lesson,
                                          @RequestParam Long tutorId,
                                          @RequestParam Long languageId) {

        User tutor = userRepository.findById(tutorId)
                .orElseThrow(() -> new RuntimeException("Tutor not found"));

        // make sure only admins can create lessons
        if (!tutor.getRole().equals("ADMIN")) {
            return ResponseEntity.badRequest().body("Only tutors can create lessons");
        }

        lesson.setTutor(tutor);
        lesson.setLanguage(languageRepository.findById(languageId)
                .orElseThrow(() -> new RuntimeException("Language not found")));

        return ResponseEntity.ok(lessonRepository.save(lesson));
    }

    // ✅ DELETE a lesson — tutor/admin only
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLesson(@PathVariable Long id) {
        lessonRepository.deleteById(id);
        return ResponseEntity.ok("Lesson deleted successfully");
    }
}