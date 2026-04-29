package com.afroverbo.backend.model;

/**
 * This Enum defines the ONLY valid states a booking can be in.
 * Using an Enum prevents typos and makes your logic much safer.
 */
public enum BookingStatus {
    PENDING,    // Student has requested, tutor hasn't accepted yet
    CONFIRMED,  // Tutor has accepted the session
    CANCELLED,  // Either party cancelled
    PAID,       // Payment has been successfully processed
    COMPLETED   // The tutoring session is finished
}