package com.ladiesapparel.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Thin wrapper around MSG91's SMS API. If msg91.enabled is false (the default, since it
 * needs a real paid account to work), every call is a safe no-op — the rest of the app
 * never has to know or care whether SMS is actually configured.
 */
@Service
@Slf4j
public class SmsService {

    @Value("${msg91.enabled:false}")
    private boolean enabled;

    @Value("${msg91.auth-key:}")
    private String authKey;

    @Value("${msg91.sender-id:LADYAP}")
    private String senderId;

    @Value("${msg91.route:4}")
    private String route;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Async
    public void sendSms(String phoneNumber, String message) {
        if (!enabled) {
            log.debug("SMS disabled (msg91.enabled=false) — skipped SMS to {}", maskPhone(phoneNumber));
            return;
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return;
        }

        try {
            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
            String url = "https://api.msg91.com/api/sendhttp.php"
                    + "?authkey=" + authKey
                    + "&mobiles=91" + phoneNumber
                    + "&message=" + encodedMessage
                    + "&sender=" + senderId
                    + "&route=" + route
                    + "&country=91";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("MSG91 SMS to {} returned status {}: {}", maskPhone(phoneNumber), response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", maskPhone(phoneNumber), e.getMessage());
        }
    }

    public void sendOtpSms(String phoneNumber, String otp) {
        sendSms(phoneNumber, "Your Ladies Apparel verification code is " + otp + ". Do not share this with anyone.");
    }

    public void sendOrderStatusSms(String phoneNumber, String orderNumber, String status) {
        sendSms(phoneNumber, "Your order " + orderNumber + " is now " + status + ". Track it in the Ladies Apparel app.");
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }
}
