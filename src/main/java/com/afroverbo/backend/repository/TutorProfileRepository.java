package com.afroverbo.backend.repository;

import com.afroverbo.backend.model.TutorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

  @Query(value = """
    SELECT tp.* FROM tutor_profiles tp
    INNER JOIN users u ON u.id = tp.user_id
    WHERE (:query IS NULL OR 
            LOWER(CAST(u.username AS TEXT)) LIKE LOWER(('%' || CAST(:query AS TEXT) || '%')) ESCAPE '' OR 
            (tp.specialization IS NOT NULL AND LOWER(CAST(tp.specialization AS TEXT)) LIKE LOWER(('%' || CAST(:query AS TEXT) || '%')) ESCAPE '') OR 
            (tp.bio IS NOT NULL AND LOWER(CAST(tp.bio AS TEXT)) LIKE LOWER(('%' || CAST(:query AS TEXT) || '%')) ESCAPE ''))
    AND (:specialization IS NULL OR LOWER(CAST(tp.specialization AS TEXT)) = LOWER(CAST(:specialization AS TEXT)))
    AND (:available IS NULL OR tp.available = :available)
    AND (:minRate IS NULL OR tp.hourly_rate >= :minRate)
    AND (:maxRate IS NULL OR tp.hourly_rate <= :maxRate)
    ORDER BY u.username ASC
    """, nativeQuery = true)
List<TutorProfile> searchTutors(@Param("query") String query,
                                @Param("specialization") String specialization,
                                @Param("available") Boolean available,
                                @Param("minRate") Double minRate,
                                @Param("maxRate") Double maxRate);
}
