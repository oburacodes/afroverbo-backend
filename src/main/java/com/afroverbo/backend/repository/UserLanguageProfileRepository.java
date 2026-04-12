package com.afroverbo.backend.repository;

import com.afroverbo.backend.model.UserLanguageProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLanguageProfileRepository extends JpaRepository<UserLanguageProfile, Long> {
    List<UserLanguageProfile> findByUserId(Long userId);
    Optional<UserLanguageProfile> findByUserIdAndLanguageId(Long userId, Long languageId);
    boolean existsByUserIdAndLanguageId(Long userId, Long languageId);
}