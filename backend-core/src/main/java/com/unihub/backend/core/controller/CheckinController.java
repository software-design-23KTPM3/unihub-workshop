package com.unihub.backend.core.controller;

import com.unihub.backend.core.model.dto.CheckinEvent;
import com.unihub.backend.core.service.CheckinService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sync")
public class CheckinController {

    private final CheckinService checkinService;

    public CheckinController(CheckinService checkinService) {
        this.checkinService = checkinService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CHECKIN_STAFF', 'ADMIN')")
    public void syncCheckins(@RequestBody List<CheckinEvent> events) {
        checkinService.syncCheckins(events);
    }
}
