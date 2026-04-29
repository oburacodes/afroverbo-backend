package com.afroverbo.backend.controller;

import com.afroverbo.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    // ── System-wide summary ───────────────────────────────────────────────────
    @GetMapping("/summary")
    public ResponseEntity<?> getSummaryReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("totalUsers",      userRepository.count());
        report.put("totalTutors",     userRepository.countByRole("ADMIN"));
        report.put("totalStudents",   userRepository.countByRole("USER"));
        report.put("totalLanguages",  languageRepository.count());
        report.put("totalLessons",    lessonRepository.count());
        report.put("totalBookings",   tutorBookingRepository.count());
        report.put("completedLessons", lessonProgressRepository.countByCompleted(true));
        report.put("languageStats",   userRepository.countUsersByLanguage());

        Double totalRevenue    = tutorBookingRepository.sumAmountPaid();
        Double platformEarnings = tutorBookingRepository.sumPlatformCut();
        report.put("totalRevenue",      totalRevenue    != null ? totalRevenue    : 0);
        report.put("platformEarnings",  platformEarnings != null ? platformEarnings : 0);
        return ResponseEntity.ok(report);
    }

    // ── Tutor-specific report ─────────────────────────────────────────────────
    @GetMapping("/tutor/{tutorId}")
    public ResponseEntity<?> getTutorReport(@PathVariable Long tutorId) {
        Map<String, Object> report = buildTutorReport(tutorId);
        return ResponseEntity.ok(report);
    }

    // ── Download as CSV ───────────────────────────────────────────────────────
    @GetMapping(value = "/tutor/{tutorId}/download", produces = "text/csv")
    public ResponseEntity<String> downloadTutorReport(@PathVariable Long tutorId) {
        Map<String, Object> report = buildTutorReport(tutorId);

        StringBuilder csv = new StringBuilder();
        csv.append("Metric,Value\n");
        appendRow(csv, "Total Bookings",        report.get("totalBookings"));
        appendRow(csv, "Confirmed Bookings",     report.get("confirmedBookings"));
        appendRow(csv, "Pending Bookings",       report.get("pendingBookings"));
        appendRow(csv, "Completed Bookings",     report.get("completedBookings"));
        appendRow(csv, "Cancelled Bookings",     report.get("cancelledBookings"));
        appendRow(csv, "Paid Bookings",          report.get("paidBookings"));
        appendRow(csv, "Total Revenue (KES)",    report.get("totalRevenue"));
        appendRow(csv, "Tutor Earnings (KES)",   report.get("tutorEarnings"));
        appendRow(csv, "Platform Cut (KES)",     report.get("platformCut"));
        appendRow(csv, "Avg Booking Value (KES)",report.get("averageBookingValue"));
        appendRow(csv, "Confirmation Rate (%)",  report.get("confirmationRate"));
        appendRow(csv, "Completion Rate (%)",    report.get("completionRate"));
        appendRow(csv, "Cancellation Rate (%)",  report.get("cancellationRate"));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=tutor-report-" + tutorId + ".csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv.toString());
    }

    // ── Private helpers ───────────────────────────────────────────────────────
    private Map<String, Object> buildTutorReport(Long tutorId) {
        Map<String, Object> report = new HashMap<>();

        long totalBookings     = tutorBookingRepository.countByTutorId(tutorId);
        long confirmedBookings = tutorBookingRepository.countByTutorIdAndStatus(tutorId, "CONFIRMED");
        long pendingBookings   = tutorBookingRepository.countByTutorIdAndStatus(tutorId, "PENDING");
        long completedBookings = tutorBookingRepository.countByTutorIdAndStatus(tutorId, "COMPLETED");
        long cancelledBookings = tutorBookingRepository.countByTutorIdAndStatus(tutorId, "CANCELLED");
        // "PAID" is a payment status — count via dedicated method
        long paidBookings      = tutorBookingRepository.countByTutorIdAndPaymentStatus(tutorId, "PAID");

        report.put("totalBookings",     totalBookings);
        report.put("confirmedBookings", confirmedBookings);
        report.put("pendingBookings",   pendingBookings);
        report.put("completedBookings", completedBookings);
        report.put("cancelledBookings", cancelledBookings);
        report.put("paidBookings",      paidBookings);

        Double totalRevenue  = tutorBookingRepository.sumAmountPaidByTutorId(tutorId);
        Double tutorEarnings = tutorBookingRepository.sumTutorCutByTutorId(tutorId);
        double rev           = totalRevenue  != null ? totalRevenue  : 0;
        double earn          = tutorEarnings != null ? tutorEarnings : 0;

        report.put("totalRevenue",   rev);
        report.put("tutorEarnings",  earn);
        report.put("platformCut",    rev - earn);

        report.put("averageBookingValue", totalBookings > 0 ? rev / totalBookings : 0);
        report.put("confirmationRate",    totalBookings > 0 ? confirmedBookings * 100.0 / totalBookings : 0);
        report.put("completionRate",      totalBookings > 0 ? completedBookings * 100.0 / totalBookings : 0);
        report.put("cancellationRate",    totalBookings > 0 ? cancelledBookings * 100.0 / totalBookings : 0);

        // Status breakdown for chart
        List<Object[]> statusBreakdown = tutorBookingRepository.getStatusBreakdown(tutorId);
        report.put("statusBreakdown", statusBreakdown.stream().map(row -> Map.of(
                "status", row[0],
                "count",  row[1]
        )).toList());

        // Last 12 months of monthly performance
        LocalDateTime twelveMonthsAgo = LocalDateTime.now().minusMonths(12);
        List<Object[]> monthly = tutorBookingRepository.getMonthlyTutorPerformance(tutorId, twelveMonthsAgo);
        report.put("monthlyPerformance", monthly.stream().map(row -> Map.of(
                "year",     row[0],
                "month",    row[1],
                "bookings", row[2],
                "earnings", row[3] != null ? row[3] : 0
        )).toList());

        // Upcoming and recent bookings
        report.put("upcomingBookings", tutorBookingRepository.searchBookings(
                null, tutorId, null, null, LocalDateTime.now(), null
        ).stream().limit(10).toList());

        report.put("recentBookings",
                tutorBookingRepository.findTop10ByTutorIdOrderBySessionDateDesc(tutorId));

        return report;
    }

    private void appendRow(StringBuilder csv, String label, Object value) {
        csv.append('"').append(label).append('"').append(',')
           .append(value != null ? value : 0).append('\n');
    }
}