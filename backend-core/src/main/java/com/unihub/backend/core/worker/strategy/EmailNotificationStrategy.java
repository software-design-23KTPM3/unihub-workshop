package com.unihub.backend.core.worker.strategy;

import com.unihub.backend.core.model.entity.Notification;
import com.unihub.backend.core.model.entity.Student;
import com.unihub.backend.core.model.entity.Workshop;
import com.unihub.backend.core.model.enums.NotificationType;
import com.unihub.backend.core.repository.StudentRepository;
import com.unihub.backend.core.repository.WorkshopRepository;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationStrategy implements NotificationStrategy {

    private final JavaMailSender javaMailSender;
    private final StudentRepository studentRepository;
    private final WorkshopRepository workshopRepository;

    private final String senderEmail = "[EMAIL_ADDRESS]";

    public EmailNotificationStrategy(JavaMailSender javaMailSender, StudentRepository studentRepository,
            WorkshopRepository workshopRepository) {
        this.javaMailSender = javaMailSender;
        this.studentRepository = studentRepository;
        this.workshopRepository = workshopRepository;
    }

    @Override
    public NotificationType getType() {
        return NotificationType.EMAIL;
    }

    @Override
    public void send(Notification notification) {
        Student student = studentRepository.findById(notification.getStudentId())
                .orElseThrow(() -> new RuntimeException("LỖI: Không tìm thấy sinh viên ID "
                        + notification.getStudentId() + " trong Database để gửi Mail!"));

        String email = student.getEmail();

        Workshop workshop = workshopRepository.findById(notification.getWorkshopId())
                .orElseThrow(() -> new RuntimeException("LỖI: Không tìm thấy sinh viên ID "
                        + notification.getWorkshopId() + " trong Database để gửi Mail!"));

        String workshopName = workshop.getName();

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom("UniHub Workshop <" + senderEmail + ">");
        msg.setTo(email);
        msg.setSubject("REGISTER Worshop: " + workshopName + " SUCCESS");
        msg.setText(notification.getContent());

        javaMailSender.send(msg);
    }
}
