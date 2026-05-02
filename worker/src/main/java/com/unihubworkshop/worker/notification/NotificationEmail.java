package com.unihubworkshop.worker.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class NotificationEmail implements NotificationChannel {
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailAddress;

    @Override
    public String getType() {
        return "EMAIL";
    }

    @Override
    public void send(NotificationData data) {
        System.out.println("EMAIL CALL");
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(mailAddress);
        mailMessage.setTo(data.getTo());
        mailMessage.setSubject(data.getTitle());
        mailMessage.setText(data.getMsg());
        mailSender.send(mailMessage);
    }
}
