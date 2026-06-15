package com.example.optimeal_api.controller;

import com.example.optimeal_api.service.MessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mess")
public class MessController {

    private final MessService messService;

    public MessController(MessService messService) {
        this.messService = messService;
    }

    @GetMapping("/status")
    public String checkStatus() {
        return messService.getStatus();
    }
}