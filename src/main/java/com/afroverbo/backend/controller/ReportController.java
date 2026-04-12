package com.afroverbo.backend.controller;

import com.afroverbo.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReportController {

    private final UserRepository userRepository;
    private final LanguageRepository languageRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final TutorBookingRepository tutorBookingRepository;

    // ✅ System-wide report (for future SUPER_ADMIN use)
    @GetMapping("/summary")
    public ResponseEntity<?> getSummaryReport() {
        Map<String, Object> report = new HashMap<>();

        long totalUsers = userRepository.count();
        report.put("totalUsers", totalUsers);

        long totalTutors = userRepository.countByRole("ADMIN");
        report.put("totalTutors", totalTutors);

        long totalStudents = userRepository.countByRole("USER");
        report.put("totalStudents", totalStudents);

        long totalLanguages = languageRepository.count();
        report.put("totalLanguages", totalLanguages);

        long totalLessons = lessonRepository.count();
        report.put("totalLessons", totalLessons);

        long totalBookings = tutorBookingRepository.count();
        report.put("totalBookings", totalBookings);

        long completedLessons = lessonProgressRepository.countByCompleted(true);
        report.put("completedLessons", completedLessons);

        List<Object[]> languageStats = userRepository.countUsersByLanguage();
        report.put("languageStats", languageStats);

        Double totalRevenue = tutorBookingRepository.sumAmountPaid();
        report.put("totalRevenue", totalRevenue != null ? totalRevenue : 0);

        Double platformEarnings = tutorBookingRepository.sumPlatformCut();
        report.put("platformEarnings", platformEarnings != null ? platformEarnings : 0);

        return ResponseEntity.ok(report);
    }

    // ✅ Tutor-specific report
    @GetMapping("/tutor/{tutorId}")
    public ResponseEntity<?> getTutorReport(@PathVariable Long tutorId) {
        Map<String, Object> report = new HashMap<>();

        long totalBookings = tutorBookingRepository.countByTutorId(tutorId);
        report.put("totalBookings", totalBookings);

        long confirmedBookings = tutorBookingRepository
                .countByTutorIdAndStatus(tutorId, "CONFIRMED");
        report.put("confirmedBookings", confirmedBookings);

        long pendingBookings = tutorBookingRepository
                .countByTutorIdAndStatus(tutorId, "PENDING");
        report.put("pendingBookings", pendingBookings);

        long completedBookings = tutorBookingRepository
                .countByTutorIdAndStatus(tutorId, "COMPLETED");
        report.put("completedBookings", completedBookings);

        long cancelledBookings = tutorBookingRepository
                .countByTutorIdAndStatus(tutorId, "CANCELLED");
        report.put("cancelledBookings", cancelledBookings);

        long paidBookings = tutorBookingRepository
                .countByTutorIdAndStatus(tutorId, "PAID");
        report.put("paidBookings", paidBookings);

        Double totalRevenue = tutorBookingRepository.sumAmountPaidByTutorId(tutorId);
        report.put("totalRevenue", totalRevenue != null ? totalRevenue : 0);

        Double tutorEarnings = tutorBookingRepository.sumTutorCutByTutorId(tutorId);
        report.put("tutorEarnings", tutorEarnings != null ? tutorEarnings : 0);

        double platformCut = (totalRevenue != null ? totalRevenue : 0)
                - (tutorEarnings != null ? tutorEarnings : 0);
        report.put("platformCut", platformCut);

        return ResponseEntity.ok(report);
    }
}