package com.sepe.mvp.controller;

import com.sepe.mvp.model.AppointmentRequest;
import com.sepe.mvp.model.ProvinceStatus;
import com.sepe.mvp.model.SearchResult;
import com.sepe.mvp.service.SepeStatusService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    public List<SearchResult> findAppointments(@RequestBody AppointmentRequest request) {
        try {
            return sepeStatusService.findProvincesForPostalCode(request.getPostalCode());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
