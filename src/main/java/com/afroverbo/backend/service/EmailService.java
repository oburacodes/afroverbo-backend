package com.afroverbo.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Afroverbo - Password Reset Code");
        message.setText(
            "Hello!\n\n" +
            "You requested a password reset for your Afroverbo account.\n\n" +
            "Your reset code is: " + otp + "\n\n" +
            "This code expires in 10 minutes.\n\n" +
            "If you did not request this, please ignore this email.\n\n" +
            "The Afroverbo Team 🌍"
        );
        mailSender.send(message);
    }

    public void sendVerificationEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Afroverbo - Verify Your Email");
        message.setText(
            "Welcome to Afroverbo! 🌍\n\n" +
            "Thank you for registering.\n\n" +
            "Your verification code is: " + otp + "\n\n" +
            "This code expires in 10 minutes.\n\n" +
            "The Afroverbo Team 🌍"
        );
        mailSender.send(message);
    }
}