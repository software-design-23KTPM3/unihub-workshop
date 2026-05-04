package com.unihub.backend.core.service.impl;

import com.unihub.backend.core.model.entity.Registration;
import com.unihub.backend.core.model.entity.Student;
import com.unihub.backend.core.model.entity.Workshop;
import com.unihub.backend.core.model.enums.RegistrationStatus;
import com.unihub.backend.core.repository.RegistrationRepository;
import com.unihub.backend.core.repository.WorkshopRepository;
import com.unihub.backend.core.service.AsyncDbService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AsyncDbServiceImpl implements AsyncDbService {

    private final RegistrationRepository registrationRepository;
    private final WorkshopRepository workshopRepository;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AsyncDbServiceImpl.class);

    public AsyncDbServiceImpl(RegistrationRepository registrationRepository, WorkshopRepository workshopRepository) {
        this.registrationRepository = registrationRepository;
        this.workshopRepository = workshopRepository;
    }

    @Override
    @Async("taskExecutor")
    @Transactional
    public void saveRegistrationAsync(Workshop workshop, Student student, UUID idempotencyKey, RegistrationStatus status) {
        try {
            log.info("Starting Async DB save for key: {}, status: {}", idempotencyKey, status);

            Registration registration = Registration.builder()
                    .id(idempotencyKey)
                    .student(student)
                    .workshop(workshop)
                    .status(status)
                    .build();

            registrationRepository.save(registration);

            workshop.setAvailableSlots(workshop.getAvailableSlots() - 1);
            workshopRepository.save(workshop);

            log.info("Async DB save COMPLETED for key: {}", idempotencyKey);

        } catch (Exception e) {
            log.error("ASYNC DB ERROR for key {}: {}", idempotencyKey, e.getMessage());
        }
    }
}