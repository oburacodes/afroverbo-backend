package com.afroverbo.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_language_profiles",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "language_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UserLanguageProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    @Column(nullable = false)
    private LocalDateTime enrolledAt = LocalDateTime.now();

    // Convenience: last time user was active on this language
    @Column
    private LocalDateTime lastActiveAt;
}