package com.sepe.mvp.controller;

import com.sepe.mvp.model.ProvinceStatus;
import com.sepe.mvp.service.SepeStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
