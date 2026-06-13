package com.learn.appjava.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void sendOrderConfirmation(String toEmail, String orderId, String productName, int quantity) {
        try {
            Context context = new Context();
            context.setVariable("orderId", orderId);
            context.setVariable("productName", productName);
            context.setVariable("quantity", quantity);

            String html = templateEngine.process("email/order-confirmation", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("✅ Đặt hàng thành công - Order #" + orderId);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Email sent to {} for order {}", toEmail, orderId);

        } catch (MessagingException e) {
            log.error("Failed to send email for order {}: {}", orderId, e.getMessage());
        }
    }
}