package com.eventara.ingestion.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Health {

    @GetMapping("/health")
    public String HealthMapping(){
        return  "The ingestion api is up and running";
    }
}
