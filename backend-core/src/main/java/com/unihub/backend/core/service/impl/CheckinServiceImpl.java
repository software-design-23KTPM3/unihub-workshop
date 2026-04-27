package com.unihub.backend.core.service.impl;

import com.unihub.backend.core.model.dto.CheckinEvent;
import com.unihub.backend.core.model.entity.Registration;
import com.unihub.backend.core.model.enums.RegistrationStatus;
import com.unihub.backend.core.repository.RegistrationRepository;
import com.unihub.backend.core.service.CheckinService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
            registrationRepository.findAll().stream()
                .filter(r -> r.getStudent().getMssv().equals(event.getStudentId()) && 
                             r.getWorkshop().getId().equals(event.getWorkshopId()) &&
                             r.getStatus() == RegistrationStatus.SUCCESS)
                .findFirst()
                .ifPresent(r -> {
                    r.setStatus(RegistrationStatus.CHECKED_IN);
                    r.setCheckedInAt(event.getCheckinAt());
                    registrationRepository.save(r);
                });
        }
    }
}
