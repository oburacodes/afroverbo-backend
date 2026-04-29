package com.afroverbo.backend.repository;

import com.afroverbo.backend.model.TutorAvailabilitySlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TutorAvailabilitySlotRepository extends JpaRepository<TutorAvailabilitySlot, Long> {

    // Updated to match new model fields (startDateTime replaces dayOfWeek + startTime)
    List<TutorAvailabilitySlot> findByTutorProfileIdOrderByStartDateTimeAsc(Long tutorProfileId);

    void deleteByTutorProfileId(Long tutorProfileId);
}