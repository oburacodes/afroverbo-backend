package com.afroverbo.backend.repository;

import com.afroverbo.backend.model.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    List<UserBadge> findByUserId(Long userId);

    // ✅ Scoped to language profile
    List<UserBadge> findByLanguageProfileId(Long languageProfileId);
    boolean existsByLanguageProfileIdAndBadgeName(Long languageProfileId, String badgeName);

    // Keep old for migration safety
    boolean existsByUserIdAndBadgeName(Long userId, String badgeName);
}