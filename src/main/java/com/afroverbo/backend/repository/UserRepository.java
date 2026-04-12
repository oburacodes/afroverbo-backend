package com.afroverbo.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.afroverbo.backend.model.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    long countByRole(String role);

    @Query("SELECT u.languageLearning.name, COUNT(u) FROM User u WHERE u.languageLearning IS NOT NULL GROUP BY u.languageLearning.name ORDER BY COUNT(u) DESC")
    List<Object[]> countUsersByLanguage();
}