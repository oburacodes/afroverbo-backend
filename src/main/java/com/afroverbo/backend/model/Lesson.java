package com.afroverbo.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title; // e.g. "Greetings in Luo"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // the actual lesson content

    @Column
    private String level; // e.g. "BEGINNER", "INTERMEDIATE", "ADVANCED"

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "language_id")
    private Language language; // which language this lesson belongs to

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tutor_id")
    private User tutor; // the admin/tutor who created this lesson
}