package com.afroverbo.backend.repository;

import com.afroverbo.backend.model.TutorBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TutorBookingRepository extends JpaRepository<TutorBooking, Long> {

    List<TutorBooking> findByStudentId(Long studentId);
    List<TutorBooking> findByTutorId(Long tutorId);
    List<TutorBooking> findByStudentIdAndStatus(Long studentId, String status);
    List<TutorBooking> findByTutorIdAndStatus(Long tutorId, String status);

    @Query("SELECT SUM(b.amountPaid) FROM TutorBooking b")
    Double sumAmountPaid();

    @Query("SELECT SUM(b.platformCut) FROM TutorBooking b")
    Double sumPlatformCut();

    // ✅ Tutor-specific queries
    @Query("SELECT SUM(b.amountPaid) FROM TutorBooking b WHERE b.tutor.id = :tutorId")
    Double sumAmountPaidByTutorId(@Param("tutorId") Long tutorId);

    @Query("SELECT SUM(b.tutorCut) FROM TutorBooking b WHERE b.tutor.id = :tutorId")
    Double sumTutorCutByTutorId(@Param("tutorId") Long tutorId);

    long countByTutorId(Long tutorId);

    long countByTutorIdAndStatus(Long tutorId, String status);
}