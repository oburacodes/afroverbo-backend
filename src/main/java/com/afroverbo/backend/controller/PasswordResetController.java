package com.afroverbo.backend.controller;

import com.afroverbo.backend.model.PasswordResetOtp;
import com.afroverbo.backend.model.User;
import com.afroverbo.backend.repository.PasswordResetOtpRepository;
import com.afroverbo.backend.repository.UserRepository;
import com.afroverbo.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@RestController
@RequestMapping("/api/password")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PasswordResetController {

    private final UserRepository userRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // ✅ Step 1 — Request OTP
    @PostMapping("/forgot")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        Optional<User> userOpt = userRepository.findAll()
                .stream()
                .filter(u -> u.getEmail().equals(email))
                .findFirst();

        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("No account found with that email");
        }

        // Delete any existing OTPs for this email
        otpRepository.deleteByEmail(email);

        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Save OTP
        PasswordResetOtp resetOtp = new PasswordResetOtp();
        resetOtp.setEmail(email);
        resetOtp.setOtp(otp);
        resetOtp.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        resetOtp.setUsed(false);
        otpRepository.save(resetOtp);

        // Send email
        emailService.sendOtpEmail(email, otp);

        return ResponseEntity.ok("Reset code sent to your email");
    }

    // ✅ Step 2 — Verify OTP and reset password
    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(
            @RequestParam String email,
            @RequestParam String otp,
            @RequestParam String newPassword) {

        Optional<PasswordResetOtp> otpOpt = otpRepository
                .findByEmailAndOtpAndUsedFalse(email, otp);

        if (otpOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid or expired code");
        }

        PasswordResetOtp resetOtp = otpOpt.get();

        // Check if OTP has expired
        if (resetOtp.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Code has expired. Please request a new one");
        }

        // Find user and update password
        Optional<User> userOpt = userRepository.findAll()
                .stream()
                .filter(u -> u.getEmail().equals(email))
                .findFirst();

        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark OTP as used
        resetOtp.setUsed(true);
        otpRepository.save(resetOtp);

        return ResponseEntity.ok("Password reset successful");
    }
}