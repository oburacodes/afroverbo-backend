package com.afroverbo.backend.controller;

import com.afroverbo.backend.model.Course;
import com.afroverbo.backend.repository.CourseRepository;
import com.afroverbo.backend.repository.LanguageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CourseController {

    private final CourseRepository courseRepository;
    private final LanguageRepository languageRepository;

    // GET all courses for a language
    @GetMapping("/language/{languageId}")
    public ResponseEntity<List<Course>> getCoursesByLanguage(@PathVariable Long languageId) {
        return ResponseEntity.ok(courseRepository.findByLanguageId(languageId));
    }

    // GET single course
    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourse(@PathVariable Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        return ResponseEntity.ok(course);
    }

    // POST create course
    @PostMapping
    public ResponseEntity<?> createCourse(@RequestBody Course course,
                                           @RequestParam Long languageId) {
        course.setLanguage(languageRepository.findById(languageId)
                .orElseThrow(() -> new RuntimeException("Language not found")));
        return ResponseEntity.ok(courseRepository.save(course));
    }

    // PUT update course
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCourse(@PathVariable Long id,
                                           @RequestBody Course updatedCourse) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        course.setTitle(updatedCourse.getTitle());
        course.setDescription(updatedCourse.getDescription());
        course.setLevel(updatedCourse.getLevel());
        course.setImageUrl(updatedCourse.getImageUrl());
        course.setEstimatedHours(updatedCourse.getEstimatedHours());
        return ResponseEntity.ok(courseRepository.save(course));
    }

    // DELETE course
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id) {
        courseRepository.deleteById(id);
        return ResponseEntity.ok("Course deleted successfully");
    }
}