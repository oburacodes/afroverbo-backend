package com.afroverbo.backend.repository;

import com.afroverbo.backend.model.BookingStatus;
import com.afroverbo.backend.model.TutorBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TutorBookingRepository extends JpaRepository<TutorBooking, Long> {

    @Query("""
        SELECT COUNT(b) > 0 FROM TutorBooking b
        WHERE b.tutor.id = :tutorId
          AND b.status NOT IN ('CANCELLED')
          AND b.sessionDate < :sessionEnd
          AND b.sessionEndDate > :sessionStart
    """)
    boolean existsConflictingBooking(
            @Param("tutorId") Long tutorId,
            @Param("sessionStart") LocalDateTime sessionStart,
            @Param("sessionEnd") LocalDateTime sessionEnd
    );

   @Query(value = "SELECT b.* FROM tutor_bookings b " +
       "JOIN users s ON s.id = b.student_id " +
       "JOIN users t ON t.id = b.tutor_id " +
       "WHERE (CAST(:studentId AS BIGINT) IS NULL OR b.student_id = :studentId) " +
       "AND (CAST(:tutorId AS BIGINT) IS NULL OR b.tutor_id = :tutorId) " +
       "AND (CAST(:status AS TEXT) IS NULL OR b.status = :status) " +
       "AND (CAST(:search AS TEXT) IS NULL OR " +
       "     s.username ILIKE '%' || :search || '%' OR " +
       "     t.username ILIKE '%' || :search || '%' OR " +
       "     COALESCE(b.notes, '') ILIKE '%' || :search || '%') " +
       "AND (CAST(:startDate AS TIMESTAMP) IS NULL OR b.session_date >= :startDate) " +
       "AND (CAST(:endDate AS TIMESTAMP) IS NULL OR b.session_date <= :endDate) " +
       "ORDER BY b.session_date DESC", nativeQuery = true)
List<TutorBooking> searchBookings(
    @Param("studentId") Long studentId,
    @Param("tutorId") Long tutorId,
    @Param("status") String status,
    @Param("search") String search,
    @Param("startDate") LocalDateTime startDate,
    @Param("endDate") LocalDateTime endDate
);

    long countByTutorId(Long tutorId);

    @Query("SELECT COUNT(b) FROM TutorBooking b WHERE b.tutor.id = :tutorId AND UPPER(CAST(b.status AS string)) = UPPER(:status)")
    long countByTutorIdAndStatus(@Param("tutorId") Long tutorId, @Param("status") String status);

    @Query("SELECT COUNT(b) FROM TutorBooking b WHERE b.tutor.id = :tutorId AND UPPER(b.paymentStatus) = UPPER(:paymentStatus)")
    long countByTutorIdAndPaymentStatus(@Param("tutorId") Long tutorId, @Param("paymentStatus") String paymentStatus);

    @Query("SELECT SUM(b.amountPaid) FROM TutorBooking b WHERE UPPER(b.paymentStatus) = 'PAID'")
    Double sumAmountPaid();

    @Query("SELECT SUM(b.platformCut) FROM TutorBooking b WHERE UPPER(b.paymentStatus) = 'PAID'")
    Double sumPlatformCut();

    @Query("SELECT SUM(b.amountPaid) FROM TutorBooking b WHERE b.tutor.id = :tutorId AND UPPER(b.paymentStatus) = 'PAID'")
    Double sumAmountPaidByTutorId(@Param("tutorId") Long tutorId);

    @Query("SELECT SUM(b.tutorCut) FROM TutorBooking b WHERE b.tutor.id = :tutorId AND UPPER(b.paymentStatus) = 'PAID'")
    Double sumTutorCutByTutorId(@Param("tutorId") Long tutorId);

    @Query("""
        SELECT b.status AS status, COUNT(b) AS count
        FROM TutorBooking b
        WHERE b.tutor.id = :tutorId
        GROUP BY b.status
    """)
    List<Object[]> getStatusBreakdown(@Param("tutorId") Long tutorId);

    @Query(value = """
        SELECT
            EXTRACT(YEAR  FROM b.session_date) AS year,
            EXTRACT(MONTH FROM b.session_date) AS month,
            COUNT(*)                           AS bookings,
            SUM(CASE
                    WHEN UPPER(b.payment_status) = 'PAID' THEN b.tutor_cut
                    ELSE 0
                END)                           AS earnings
        FROM tutor_bookings b
        WHERE b.tutor_id = :tutorId
          AND b.session_date >= :from
        GROUP BY year, month
        ORDER BY year ASC, month ASC
    """, nativeQuery = true)
    List<Object[]> getMonthlyTutorPerformance(
            @Param("tutorId") Long tutorId,
            @Param("from")    LocalDateTime from
    );

    TutorBooking findByCheckoutRequestId(String checkoutRequestId);

    List<TutorBooking> findTop10ByTutorIdOrderBySessionDateDesc(Long tutorId);
}