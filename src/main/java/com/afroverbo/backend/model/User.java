package com.afroverbo.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String role;

    // Initial language chosen at signup (kept for backward compat)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "language_id")
    private Language languageLearning;

    // ✅ Currently active language (changes when user switches)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "active_language_id")
    private Language activeLanguage;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean verified = false;

    public boolean isVerified() {
        return Boolean.TRUE.equals(this.verified);
    }
}