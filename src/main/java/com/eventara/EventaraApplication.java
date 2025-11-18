package com.eventara;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.eventara")
public class EventaraApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventaraApplication.class, args);
    }
}
