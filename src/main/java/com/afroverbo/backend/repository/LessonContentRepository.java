package com.afroverbo.backend.repository;

import com.afroverbo.backend.model.LessonContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonContentRepository extends JpaRepository<LessonContent, Long> {
    List<LessonContent> findByModuleIdOrderByOrderNumber(Long moduleId);
}    