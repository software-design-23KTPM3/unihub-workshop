package com.unihub.backend.core.service;

import com.unihub.backend.core.model.dto.CheckinEvent;
import com.unihub.backend.core.model.dto.CheckinSyncResponse;
import java.util.List;

public interface CheckinService {
    CheckinSyncResponse syncCheckins(List<CheckinEvent> events);
}
