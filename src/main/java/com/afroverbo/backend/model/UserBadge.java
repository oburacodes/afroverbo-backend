package com.afroverbo.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_badges")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    // ✅ Scoped to a language profile
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "language_profile_id")
    private UserLanguageProfile languageProfile;

    @Column(nullable = false)
    private String badgeName;

    @Column(nullable = false)
    private String badgeIcon;

    @Column
    private String description;

    @Column
    private LocalDateTime earnedAt;
}