package com.afroverbo.backend.controller;

import com.afroverbo.backend.model.Language;
import com.afroverbo.backend.model.PasswordResetOtp;
import com.afroverbo.backend.repository.LanguageRepository;
import com.afroverbo.backend.repository.PasswordResetOtpRepository;
import com.afroverbo.backend.repository.UserLanguageProfileRepository;
import com.afroverbo.backend.security.JwtUtil;
import com.afroverbo.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.afroverbo.backend.model.User;
import com.afroverbo.backend.model.UserLanguageProfile;
import com.afroverbo.backend.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final LanguageRepository languageRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final PasswordResetOtpRepository otpRepository;
    private final UserLanguageProfileRepository languageProfileRepository;    


    @PostMapping("/register")
public ResponseEntity<?> register(@RequestBody User user,
                                  @RequestParam(required = false) Long languageId) {

    if (userRepository.existsByUsername(user.getUsername())) {
        return ResponseEntity.badRequest().body("Username already exists");
    }
    if (userRepository.existsByEmail(user.getEmail())) {
        return ResponseEntity.badRequest().body("Email already exists");
    }

    Language selectedLanguage = null;
    if (languageId != null) {
        selectedLanguage = languageRepository.findById(languageId)
                .orElseThrow(() -> new RuntimeException("Language not found"));
        user.setLanguageLearning(selectedLanguage);
        user.setActiveLanguage(selectedLanguage); // ✅ set active too
    }

    user.setPassword(passwordEncoder.encode(user.getPassword()));
    user.setVerified(false);
    User savedUser = userRepository.save(user);

    // ✅ Create language profile for initial language
    if (selectedLanguage != null) {
        UserLanguageProfile profile = new UserLanguageProfile();
        profile.setUser(savedUser);
        profile.setLanguage(selectedLanguage);
        profile.setEnrolledAt(LocalDateTime.now());
        languageProfileRepository.save(profile);
    }

    // OTP flow unchanged
    otpRepository.deleteByEmail(savedUser.getEmail());
    String otp = String.format("%06d", new Random().nextInt(999999));
    PasswordResetOtp verificationOtp = new PasswordResetOtp();
    verificationOtp.setEmail(savedUser.getEmail());
    verificationOtp.setOtp(otp);
    verificationOtp.setExpiresAt(LocalDateTime.now().plusMinutes(10));
    verificationOtp.setUsed(false);
    otpRepository.save(verificationOtp);
    emailService.sendVerificationEmail(savedUser.getEmail(), otp);

    return ResponseEntity.ok("Verification code sent to " + savedUser.getEmail());
}
    // ✅ VERIFY EMAIL
    @PostMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String email,
                                          @RequestParam String otp) {

        Optional<PasswordResetOtp> otpOpt = otpRepository
                .findByEmailAndOtpAndUsedFalse(email, otp);

        if (otpOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid or expired code");
        }

        PasswordResetOtp verificationOtp = otpOpt.get();

        if (verificationOtp.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Code has expired. Please register again");
        }

        Optional<User> userOpt = userRepository.findAll()
                .stream()
                .filter(u -> u.getEmail().equals(email))
                .findFirst();

        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = userOpt.get();
        user.setVerified(true);
        userRepository.save(user);

        verificationOtp.setUsed(true);
        otpRepository.save(verificationOtp);

        String token = jwtUtil.generateToken(user.getUsername());
        return ResponseEntity.ok(token);
    }

    // 🔐 LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {

        Optional<User> userOpt = userRepository.findByUsername(user.getUsername());
        if (userOpt.isPresent() && !Boolean.TRUE.equals(userOpt.get().getVerified())) {
            return ResponseEntity.status(403).body("Please verify your email first");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        user.getPassword()
                )
        );

        String token = jwtUtil.generateToken(user.getUsername());
        return ResponseEntity.ok(token);
    }
}