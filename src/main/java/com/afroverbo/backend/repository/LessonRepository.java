package com.afroverbo.backend.repository;

import com.afroverbo.backend.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {
    
    // Get all lessons by language
    List<Lesson> findByLanguageId(Long languageId);

    // Get all lessons by tutor
    List<Lesson> findByTutorId(Long tutorId);

    // Get lessons by language and level
    List<Lesson> findByLanguageIdAndLevel(Long languageId, String level);
}