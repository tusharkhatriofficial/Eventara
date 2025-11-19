package com.eventara;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.eventara")
@EnableScheduling
public class EventaraApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventaraApplication.class, args);
    }
}
