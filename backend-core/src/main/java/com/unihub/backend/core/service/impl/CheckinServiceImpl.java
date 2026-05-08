package com.unihub.backend.core.service.impl;

import com.unihub.backend.core.model.dto.CheckinEvent;
import com.unihub.backend.core.model.entity.Registration;
import com.unihub.backend.core.model.enums.RegistrationStatus;
import com.unihub.backend.core.repository.RegistrationRepository;
import com.unihub.backend.core.service.CheckinService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CheckinServiceImpl implements CheckinService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CheckinServiceImpl.class);

    private final RegistrationRepository registrationRepository;

    public CheckinServiceImpl(RegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    @Override
    @Transactional
    public void syncCheckins(List<CheckinEvent> events) {
        log.info("Syncing {} check-in events", events.size());
        
        for (CheckinEvent event : events) {
            findRegistration(event)
                .filter(r -> r.getStatus() == RegistrationStatus.SUCCESS)
                .ifPresentOrElse(r -> {
                    r.setStatus(RegistrationStatus.CHECKED_IN);
                    r.setCheckedInAt(event.getCheckinAt());
                    registrationRepository.save(r);
                }, () -> {
                    log.warn("Ignored check-in event {}: registration not found or not in SUCCESS status",
                            event.getClientEventId());
                });
        }
    }

    private Optional<Registration> findRegistration(CheckinEvent event) {
        if (event.getRegistrationId() != null) {
            return registrationRepository.findById(event.getRegistrationId());
        }

        UUID registrationIdFromQr = parseUuid(event.getQrCode());
        if (registrationIdFromQr != null) {
            return registrationRepository.findById(registrationIdFromQr);
        }

        if (event.getQrCode() != null && !event.getQrCode().isBlank()) {
            return registrationRepository.findByQrCode(event.getQrCode());
        }

        if (event.getStudentId() == null || event.getWorkshopId() == null) {
            return Optional.empty();
        }

        return registrationRepository.findByStudentMssvAndWorkshopId(event.getStudentId(), event.getWorkshopId());
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
