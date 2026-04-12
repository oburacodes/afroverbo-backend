package com.afroverbo.backend.controller;

import com.afroverbo.backend.model.Module;
import com.afroverbo.backend.repository.CourseRepository;
import com.afroverbo.backend.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/modules")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ModuleController {

    private final ModuleRepository moduleRepository;
    private final CourseRepository courseRepository;

    // GET all modules for a course
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<Module>> getModulesByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(moduleRepository.findByCourseIdOrderByOrderNumber(courseId));
    }

    // GET single module
    @GetMapping("/{id}")
    public ResponseEntity<Module> getModule(@PathVariable Long id) {
        Module module = moduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Module not found"));
        return ResponseEntity.ok(module);
    }

    // POST create module
    @PostMapping
    public ResponseEntity<?> createModule(@RequestBody Module module,
                                           @RequestParam Long courseId) {
        module.setCourse(courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found")));
        return ResponseEntity.ok(moduleRepository.save(module));
    }

    // PUT update module
    @PutMapping("/{id}")
    public ResponseEntity<?> updateModule(@PathVariable Long id,
                                           @RequestBody Module updatedModule) {
        Module module = moduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Module not found"));
        module.setTitle(updatedModule.getTitle());
        module.setDescription(updatedModule.getDescription());
        module.setOrderNumber(updatedModule.getOrderNumber());
        return ResponseEntity.ok(moduleRepository.save(module));
    }

    // DELETE module
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteModule(@PathVariable Long id) {
        moduleRepository.deleteById(id);
        return ResponseEntity.ok("Module deleted successfully");
    }
}