package com.afroverbo.backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "languages")
public class Language {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g. "Luo", "Kikuyu", "Zulu"

    @Column
    private String description; // e.g. "A Nilotic language spoken in Western Kenya"

    @Column
    private String flagEmoji; // e.g. "🇰🇪" just for fun in the UI
}