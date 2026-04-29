package com.afroverbo.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_progress")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UserProgress {

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lesson_id")
    private LessonContent lesson;

    @Column
    private boolean completed = false;

    @Column
    private Integer quizScore;

    @Column
    private Integer bestQuizScore;

    @Column
    private Integer attempts = 0;

    @Column
    private LocalDateTime completedAt;

    @Column
    private LocalDateTime lastAttemptAt;
}
