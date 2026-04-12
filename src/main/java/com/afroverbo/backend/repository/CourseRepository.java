package com.afroverbo.backend.repository;

import com.afroverbo.backend.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByLanguageId(Long languageId);
    List<Course> findByLanguageIdAndLevel(Long languageId, String level);
}