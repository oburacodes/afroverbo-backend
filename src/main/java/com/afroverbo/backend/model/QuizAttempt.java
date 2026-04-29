package com.afroverbo.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_progress_id", nullable = false)
    private UserProgress userProgress;

    @Column(nullable = false)
    private Integer attemptNumber;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false)
    private boolean completed;

    @Column(nullable = false)
    private LocalDateTime attemptedAt;
}
