package com.afroverbo.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lesson_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user; // the student

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson; // the lesson completed

    @Column(nullable = false)
    private boolean completed; // true when student finishes lesson

    @Column
    private int score; // quiz score out of 100 (we'll use this in Phase 5)

    @Column
    private LocalDateTime completedAt; // when they completed it
}