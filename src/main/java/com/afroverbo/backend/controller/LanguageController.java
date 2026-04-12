package com.afroverbo.backend.controller;

import com.afroverbo.backend.model.Language;
import com.afroverbo.backend.repository.LanguageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/languages")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LanguageController {

    private final LanguageRepository languageRepository;

    // ✅ GET all languages — used in Flutter registration dropdown
    @GetMapping
    public ResponseEntity<List<Language>> getAllLanguages() {
        return ResponseEntity.ok(languageRepository.findAll());
    }

    // ✅ POST add a new language — admin only (we'll restrict this later)
    @PostMapping
    public ResponseEntity<Language> addLanguage(@RequestBody Language language) {
        return ResponseEntity.ok(languageRepository.save(language));
    }
}