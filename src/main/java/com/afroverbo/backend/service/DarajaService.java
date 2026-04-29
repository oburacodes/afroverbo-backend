package com.afroverbo.backend.service;

import com.afroverbo.backend.config.DarajaProperties;
import com.afroverbo.backend.model.BookingStatus;
import com.afroverbo.backend.model.TutorBooking;
import com.afroverbo.backend.repository.TutorBookingRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DarajaService {

    private final DarajaProperties darajaProperties;
    private final TutorBookingRepository tutorBookingRepository;

    // Not a Spring bean in this project — instantiate directly
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final DateTimeFormatter DARAJA_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ── STK Push entry point ──────────────────────────────────────────────────
    public Map<String, Object> initiateStkPush(TutorBooking booking, String phoneNumber) {
        if (darajaProperties.isMockEnabled()) {
            return mockPayment(booking, phoneNumber);
        }
        validateCredentials();
        return realStkPush(booking, phoneNumber);
    }

    // ── Callback handler ──────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public void handleCallback(Map<String, Object> payload) {
        try {
            Map<String, Object> body = (Map<String, Object>) payload.get("Body");
            if (body == null || !body.containsKey("stkCallback")) return;

            Map<String, Object> callback = (Map<String, Object>) body.get("stkCallback");
            String checkoutRequestId = (String) callback.get("CheckoutRequestID");

            TutorBooking booking = tutorBookingRepository.findByCheckoutRequestId(checkoutRequestId);
            if (booking == null) return;

            Number resultCode = (Number) callback.get("ResultCode");
            if (resultCode != null && resultCode.intValue() == 0) {
                booking.setPaymentStatus("PAID");
                booking.setStatus(BookingStatus.CONFIRMED);   // auto-confirm on successful payment
                booking.setPaidAt(LocalDateTime.now());

                Object metadataObject = callback.get("CallbackMetadata");
                if (metadataObject instanceof Map<?, ?> metadata) {
                    Object itemsObject = metadata.get("Item");
                    if (itemsObject instanceof Iterable<?> items) {
                        for (Object item : items) {
                            if (item instanceof Map<?, ?> entry) {
                                Object name  = entry.get("Name");
                                Object value = entry.get("Value");
                                if ("MpesaReceiptNumber".equals(name) && value != null) {
                                    booking.setMpesaReceiptNumber(String.valueOf(value));
                                }
                                if ("Amount".equals(name) && value instanceof Number num) {
                                    booking.setAmountPaid(num.doubleValue());
                                }
                            }
                        }
                    }
                }
            } else {
                booking.setPaymentStatus("FAILED");
            }
            tutorBookingRepository.save(booking);
        } catch (Exception ignored) {
            // silently absorb — Safaricom expects HTTP 200 regardless
        }
    }

    // ── Real STK Push ─────────────────────────────────────────────────────────
    private Map<String, Object> realStkPush(TutorBooking booking, String phoneNumber) {
        try {
            String accessToken = getAccessToken();
            String timestamp   = LocalDateTime.now().format(DARAJA_TIMESTAMP);
            String password    = Base64.getEncoder().encodeToString(
                    (darajaProperties.getShortCode()
                     + darajaProperties.getPasskey()
                     + timestamp).getBytes(StandardCharsets.UTF_8));

            Map<String, Object> payload = new HashMap<>();
            payload.put("BusinessShortCode", darajaProperties.getShortCode());
            payload.put("Password",          password);
            payload.put("Timestamp",         timestamp);
            payload.put("TransactionType",   darajaProperties.getTransactionType());
            payload.put("Amount",            booking.getAmountPaid() != null
                                                ? booking.getAmountPaid().intValue() : 0);
            payload.put("PartyA",            phoneNumber);
            payload.put("PartyB",            darajaProperties.getShortCode());
            payload.put("PhoneNumber",       phoneNumber);
            payload.put("CallBackURL",       darajaProperties.getCallbackUrl());
            payload.put("AccountReference",  "BOOKING-" + booking.getId());
            payload.put("TransactionDesc",   "Afroverbo tutor session");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(darajaProperties.getBaseUrl()
                            + "/mpesa/stkpush/v1/processrequest"))
                    .header("Content-Type",  "application/json")
                    .header("Authorization", "Bearer " + accessToken)
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Daraja STK push failed: HTTP " + response.statusCode());
            }

            Map<String, Object> body =
                    objectMapper.readValue(response.body(), new TypeReference<>() {});

            booking.setPhoneNumber(phoneNumber);
            booking.setPaymentStatus("PENDING");
            booking.setMerchantRequestId((String) body.get("MerchantRequestID"));
            booking.setCheckoutRequestId((String) body.get("CheckoutRequestID"));
            booking.setPaymentReference("STK_PUSH");
            tutorBookingRepository.save(booking);
            return body;

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Unable to initiate Daraja payment", ex);
        }
    }

    // ── Mock (local dev / sandbox without real creds) ─────────────────────────
    private Map<String, Object> mockPayment(TutorBooking booking, String phoneNumber) {
        booking.setPhoneNumber(phoneNumber);
        booking.setPaymentStatus("PAID"); // This stays as a String (it's fine!)
        booking.setStatus(BookingStatus.CONFIRMED); // Remove quotes, use the Enum
        booking.setPaymentReference("MOCK-" + UUID.randomUUID());
        booking.setMpesaReceiptNumber("MOCK" + booking.getId());
        booking.setPaidAt(LocalDateTime.now());
        tutorBookingRepository.save(booking);

        Map<String, Object> response = new HashMap<>();
        response.put("MerchantRequestID",    "MOCK-" + booking.getId());
        response.put("CheckoutRequestID",    "MOCK-CHECKOUT-" + booking.getId());
        response.put("ResponseCode",         "0");
        response.put("ResponseDescription",  "Mock payment completed successfully");
        response.put("CustomerMessage",      "Success (Mock Mode)");
        return response;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String getAccessToken() throws Exception {
        String auth = Base64.getEncoder().encodeToString(
                (darajaProperties.getConsumerKey() + ":"
                 + darajaProperties.getConsumerSecret())
                        .getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(darajaProperties.getBaseUrl()
                        + "/oauth/v1/generate?grant_type=client_credentials"))
                .header("Authorization", "Basic " + auth)
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Failed to get Daraja access token: HTTP " + response.statusCode());
        }

        Map<String, Object> body =
                objectMapper.readValue(response.body(), new TypeReference<>() {});
        Object token = body.get("access_token");
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Daraja access token missing in response");
        }
        return token.toString();
    }

    private void validateCredentials() {
        if (isBlank(darajaProperties.getConsumerKey())
                || isBlank(darajaProperties.getConsumerSecret())
                || isBlank(darajaProperties.getShortCode())
                || isBlank(darajaProperties.getPasskey())
                || isBlank(darajaProperties.getCallbackUrl())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Daraja credentials are not configured. "
                    + "Set DARAJA_* environment variables or set daraja.mock-enabled=true.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}