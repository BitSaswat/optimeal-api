package com.example.optimeal_api.service;

import org.springframework.stereotype.Service;

@Service
public class MessService {

    public String getStatus() {
        return "System Operational: Rolling deadlines active.";
    }

    // You will add your opt-out toggle logic here later.
}