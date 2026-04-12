package com.afroverbo.backend.controller;

import com.afroverbo.backend.model.LessonContent;
import com.afroverbo.backend.repository.LessonContentRepository;
import com.afroverbo.backend.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lesson-contents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LessonContentController {

    private final LessonContentRepository lessonContentRepository;
    private final ModuleRepository moduleRepository;

    // GET all lessons for a module
    @GetMapping("/module/{moduleId}")
    public ResponseEntity<List<LessonContent>> getLessonsByModule(@PathVariable Long moduleId) {
        return ResponseEntity.ok(lessonContentRepository.findByModuleIdOrderByOrderNumber(moduleId));
    }

    // GET single lesson
    @GetMapping("/{id}")
    public ResponseEntity<LessonContent> getLesson(@PathVariable Long id) {
        LessonContent lesson = lessonContentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));
        return ResponseEntity.ok(lesson);
    }

    // POST create lesson
    @PostMapping
    public ResponseEntity<?> createLesson(@RequestBody LessonContent lesson,
                                           @RequestParam Long moduleId) {
        lesson.setModule(moduleRepository.findById(moduleId)
                .orElseThrow(() -> new RuntimeException("Module not found")));
        return ResponseEntity.ok(lessonContentRepository.save(lesson));
    }

    // PUT update lesson
    @PutMapping("/{id}")
    public ResponseEntity<?> updateLesson(@PathVariable Long id,
                                           @RequestBody LessonContent updatedLesson) {
        LessonContent lesson = lessonContentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));
        lesson.setTitle(updatedLesson.getTitle());
        lesson.setContent(updatedLesson.getContent());
        lesson.setVocabulary(updatedLesson.getVocabulary());
        lesson.setCulturalNotes(updatedLesson.getCulturalNotes());
        lesson.setAudioUrl(updatedLesson.getAudioUrl());
        lesson.setOrderNumber(updatedLesson.getOrderNumber());
        return ResponseEntity.ok(lessonContentRepository.save(lesson));
    }

    // DELETE lesson
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLesson(@PathVariable Long id) {
        lessonContentRepository.deleteById(id);
        return ResponseEntity.ok("Lesson deleted successfully");
    }
}