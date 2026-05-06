package com.unihub.backend.core.service.impl;

import com.unihub.backend.core.model.entity.*;
import com.unihub.backend.core.model.enums.RegistrationStatus;
import com.unihub.backend.core.model.enums.TransactionStatus;
import com.unihub.backend.core.repository.*;
import com.unihub.backend.core.service.AsyncDbService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AsyncDbServiceImpl implements AsyncDbService {

    private final RegistrationRepository registrationRepository;
    private final WorkshopRepository workshopRepository;
    private final TransactionRepository transactionRepository;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AsyncDbServiceImpl.class);

    public AsyncDbServiceImpl(RegistrationRepository registrationRepository,
            WorkshopRepository workshopRepository,
            TransactionRepository transactionRepository) {
        this.registrationRepository = registrationRepository;
        this.workshopRepository = workshopRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Async("taskExecutor")
    @Transactional
    public void saveRegistrationAsync(Workshop workshop, Student student, UUID idempotencyKey,
            RegistrationStatus status) {
        try {
            log.info("Starting Async DB save for key: {}, status: {}", idempotencyKey, status);

            Registration registration = Registration.builder()
                    .id(idempotencyKey)
                    .student(student)
                    .workshop(workshop)
                    .status(status)
                    .build();

            // Sử dụng saveAndFlush để đảm bảo bản ghi Registration tồn tại trong DB trước khi Transaction tham chiếu tới
            registration = registrationRepository.saveAndFlush(registration);

            // Nếu workshop có phí, tạo sẵn bản ghi giao dịch (Transaction) ở trạng thái
            // PENDING
            if (Boolean.TRUE.equals(workshop.getIsPaid())) {
                Transaction transaction = Transaction.builder()
                        .registration(registration)
                        .amount(workshop.getPrice())
                        .status(TransactionStatus.PENDING)
                        .idempotencyKey(UUID.randomUUID())
                        .build();
                transactionRepository.save(transaction);
                log.info("Transaction PENDING created for registration: {}", idempotencyKey);
            }

            workshop.setAvailableSlots(workshop.getAvailableSlots() - 1);
            workshopRepository.save(workshop);

            log.info("Async DB save COMPLETED for key: {}", idempotencyKey);

        } catch (Exception e) {
            log.error("ASYNC DB ERROR for key {}: {}", idempotencyKey, e.getMessage());
        }
    }
}