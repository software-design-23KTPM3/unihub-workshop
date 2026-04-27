package com.unihub.backend.core.worker;

import com.unihub.backend.core.model.dto.RegistrationRequest;
import com.unihub.backend.core.model.entity.Registration;
import com.unihub.backend.core.model.entity.Student;
import com.unihub.backend.core.model.entity.Workshop;
import com.unihub.backend.core.model.enums.RegistrationStatus;
import com.unihub.backend.core.repository.RegistrationRepository;
import com.unihub.backend.core.repository.StudentRepository;
import com.unihub.backend.core.repository.WorkshopRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.unihub.backend.core.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@Component
public class RegistrationWorker {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RegistrationWorker.class);

    private final RegistrationRepository registrationRepository;
    private final WorkshopRepository workshopRepository;
    private final StudentRepository studentRepository;
    private final RabbitTemplate rabbitTemplate;

    public RegistrationWorker(RegistrationRepository registrationRepository,
            WorkshopRepository workshopRepository,
            StudentRepository studentRepository,
            RabbitTemplate rabbitTemplate) {
        this.registrationRepository = registrationRepository;
        this.workshopRepository = workshopRepository;
        this.studentRepository = studentRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "registration.queue")
    @Transactional
    public void handleRegistration(RegistrationRequest request) {
        log.info("Processing registration for student {} and workshop {}", request.getStudentId(),
                request.getWorkshopId());

        try {
            Workshop workshop = workshopRepository.findById(request.getWorkshopId())
                    .orElseThrow(() -> new RuntimeException("Workshop not found"));
            Student student = studentRepository.findById(request.getStudentId())
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            Registration registration = Registration.builder()
                    .idempotencyKey(request.getIdempotencyKey())
                    .student(student)
                    .workshop(workshop)
                    .status(RegistrationStatus.SUCCESS)
                    .build();

            registrationRepository.save(registration);

            workshop.setAvailableSlots(workshop.getAvailableSlots() - 1);
            workshopRepository.save(workshop);

            log.info("Registration successful for student {} and workshop {}", request.getStudentId(),
                    request.getWorkshopId());

            rabbitTemplate.convertAndSend(
                    RabbitConfig.REGISTRATION_EXCHANGE,
                    RabbitConfig.NOTIFICATION_ROUTING_KEY,
                    request);

            log.info("DB Saved. Sent notification trigger for student {}", request.getStudentId());

        } catch (Exception e) {
            log.error("Failed to process registration", e);
            throw e;
        }
    }
}
