package com.afroverbo.backend.repository;

import com.afroverbo.backend.model.UserProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {
    List<UserProgress> findByUserId(Long userId);

    // ✅ Scoped to language profile
    List<UserProgress> findByLanguageProfileId(Long languageProfileId);
    Optional<UserProgress> findByLanguageProfileIdAndLessonId(Long languageProfileId, Long lessonId);
    long countByLanguageProfileIdAndCompleted(Long languageProfileId, boolean completed);

    // Keep old ones for migration safety
    Optional<UserProgress> findByUserIdAndLessonId(Long userId, Long lessonId);
    long countByUserIdAndCompleted(Long userId, boolean completed);
}