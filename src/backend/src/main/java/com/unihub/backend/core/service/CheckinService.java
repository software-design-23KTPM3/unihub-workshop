package com.unihub.backend.core.service;

import com.unihub.backend.core.model.dto.CheckinEvent;
import java.util.List;

public interface CheckinService {
    void syncCheckins(List<CheckinEvent> events);
}
