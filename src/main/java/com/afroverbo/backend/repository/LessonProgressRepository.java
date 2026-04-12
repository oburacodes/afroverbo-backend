package com.afroverbo.backend.repository;

import com.afroverbo.backend.model.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    List<LessonProgress> findByUserId(Long userId);
    List<LessonProgress> findByUserIdAndCompleted(Long userId, boolean completed);
    Optional<LessonProgress> findByUserIdAndLessonId(Long userId, Long lessonId);
    int countByUserIdAndCompleted(Long userId, boolean completed);
    long countByCompleted(boolean completed);
}