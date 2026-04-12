package com.afroverbo.backend.repository;

import com.afroverbo.backend.model.TutorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TutorProfileRepository extends JpaRepository<TutorProfile, Long> {

    // Get tutor profile by user id
    Optional<TutorProfile> findByUserId(Long userId);

    // Get all available tutors
    List<TutorProfile> findByAvailable(boolean available);

    // Get tutors by specialization (language)
    List<TutorProfile> findBySpecialization(String specialization);

    // Get available tutors by specialization
    List<TutorProfile> findBySpecializationAndAvailable(String specialization, boolean available);
}