package com.ladiesapparel.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendOtpEmail(String toEmail, String otpCode, String purposeLabel) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Your OTP for " + purposeLabel + " - Ladies Apparel");
            message.setText(
                    "Hi,\n\n" +
                    "Your One-Time Password (OTP) is: " + otpCode + "\n" +
                    "This OTP is valid for a limited time. Do not share it with anyone.\n\n" +
                    "If you did not request this, please ignore this email.\n\n" +
                    "Regards,\nLadies Apparel Team"
            );
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendOrderConfirmationEmail(String toEmail, String orderNumber, String totalAmount) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Order Confirmed - " + orderNumber);
            message.setText(
                    "Hi,\n\n" +
                    "Thank you for shopping with us! Your order " + orderNumber +
                    " worth Rs. " + totalAmount + " has been confirmed.\n\n" +
                    "We will notify you once it is shipped.\n\n" +
                    "Regards,\nLadies Apparel Team"
            );
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send order confirmation email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendOrderStatusUpdateEmail(String toEmail, String orderNumber, String newStatus) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Update on your order " + orderNumber);
            message.setText(
                    "Hi,\n\n" +
                    "Your order " + orderNumber + " status has been updated to: " + newStatus + ".\n\n" +
                    "You can track your order anytime from the Orders section of your account.\n\n" +
                    "Regards,\nLadies Apparel Team"
            );
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send order status email to {}: {}", toEmail, e.getMessage());
        }
    }
}
