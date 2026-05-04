package com.unihub.backend.core.service;

import com.unihub.backend.core.model.entity.Student;
import com.unihub.backend.core.model.entity.Workshop;
import com.unihub.backend.core.model.enums.RegistrationStatus;
import java.util.UUID;

public interface AsyncDbService {
    void saveRegistrationAsync(Workshop workshop, Student student, UUID idempotencyKey, RegistrationStatus status);
}
