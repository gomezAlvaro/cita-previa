package com.sepe.mvp.controller;

import com.sepe.mvp.model.AppointmentRequest;
import com.sepe.mvp.model.OfficeAvailability;
import com.sepe.mvp.model.ProvinceStatus;
import com.sepe.mvp.service.SepeStatusService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SepeStatusController {

    private final SepeStatusService sepeStatusService;

    public SepeStatusController(SepeStatusService sepeStatusService) {
        this.sepeStatusService = sepeStatusService;
    }

    @GetMapping("/status")
    public List<ProvinceStatus> getStatus() {
        return sepeStatusService.getAllProvincesStatus();
    }

    @PostMapping("/find-appointments")
    public List<OfficeAvailability> findAppointments(@RequestBody AppointmentRequest request) {
        return sepeStatusService.findAvailableOffices(request);
    }
}
