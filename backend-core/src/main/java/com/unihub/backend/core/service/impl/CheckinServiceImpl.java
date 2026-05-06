package com.unihub.backend.core.service.impl;

import com.unihub.backend.core.model.dto.CheckinEvent;
import com.unihub.backend.core.model.dto.CheckinResult;
import com.unihub.backend.core.model.dto.CheckinSyncResponse;
import com.unihub.backend.core.model.entity.Registration;
import com.unihub.backend.core.model.enums.RegistrationStatus;
import com.unihub.backend.core.repository.RegistrationRepository;
import com.unihub.backend.core.service.CheckinService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CheckinServiceImpl implements CheckinService {

    private static final String STATUS_CHECKED_IN = "CHECKED_IN";
    private static final String STATUS_ALREADY_CHECKED_IN = "ALREADY_CHECKED_IN";
    private static final String STATUS_NOT_FOUND = "NOT_FOUND";
    private static final String STATUS_INVALID_STATUS = "INVALID_STATUS";
    private static final String STATUS_INVALID_PAYLOAD = "INVALID_PAYLOAD";

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CheckinServiceImpl.class);

    private final RegistrationRepository registrationRepository;

    public CheckinServiceImpl(RegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    @Override
    @Transactional
    public CheckinSyncResponse syncCheckins(List<CheckinEvent> events) {
        if (events == null || events.isEmpty()) {
            return CheckinSyncResponse.builder()
                    .total(0)
                    .success(0)
                    .failed(0)
                    .results(List.of())
                    .build();
        }

        log.info("Syncing {} check-in events", events.size());

        List<CheckinResult> results = new ArrayList<>();
        int success = 0;
        int failed = 0;

        for (CheckinEvent event : events) {
            CheckinResult result = processCheckinEvent(event);
            results.add(result);

            if (STATUS_CHECKED_IN.equals(result.getStatus()) || STATUS_ALREADY_CHECKED_IN.equals(result.getStatus())) {
                success++;
            } else {
                failed++;
            }
        }

        return CheckinSyncResponse.builder()
                .total(events.size())
                .success(success)
                .failed(failed)
                .results(results)
                .build();
    }

    private CheckinResult processCheckinEvent(CheckinEvent event) {
        if (event == null || isBlank(event.getStudentId()) || event.getWorkshopId() == null
                || event.getCheckinAt() == null) {
            return CheckinResult.builder()
                    .studentId(event != null ? event.getStudentId() : null)
                    .workshopId(event != null ? event.getWorkshopId() : null)
                    .status(STATUS_INVALID_PAYLOAD)
                    .message("Missing studentId, workshopId, or checkinAt.")
                    .build();
        }

        Optional<Registration> registrationOptional = registrationRepository.findByStudentMssvAndWorkshopId(
                event.getStudentId().trim(),
                event.getWorkshopId());

        if (registrationOptional.isEmpty()) {
            return CheckinResult.builder()
                    .studentId(event.getStudentId())
                    .workshopId(event.getWorkshopId())
                    .status(STATUS_NOT_FOUND)
                    .message("Registration was not found for this student and workshop.")
                    .build();
        }

        Registration registration = registrationOptional.get();
        if (registration.getStatus() == RegistrationStatus.CHECKED_IN) {
            return CheckinResult.builder()
                    .studentId(event.getStudentId())
                    .workshopId(event.getWorkshopId())
                    .registrationId(registration.getId())
                    .status(STATUS_ALREADY_CHECKED_IN)
                    .message("Registration was already checked in.")
                    .checkedInAt(registration.getCheckedInAt())
                    .build();
        }

        if (registration.getStatus() != RegistrationStatus.SUCCESS) {
            return CheckinResult.builder()
                    .studentId(event.getStudentId())
                    .workshopId(event.getWorkshopId())
                    .registrationId(registration.getId())
                    .status(STATUS_INVALID_STATUS)
                    .message("Registration status does not allow check-in: " + registration.getStatus())
                    .checkedInAt(registration.getCheckedInAt())
                    .build();
        }

        registration.setStatus(RegistrationStatus.CHECKED_IN);
        registration.setCheckedInAt(event.getCheckinAt());
        registrationRepository.save(registration);

        return CheckinResult.builder()
                .studentId(event.getStudentId())
                .workshopId(event.getWorkshopId())
                .registrationId(registration.getId())
                .status(STATUS_CHECKED_IN)
                .message("Check-in successful.")
                .checkedInAt(event.getCheckinAt())
                .build();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
