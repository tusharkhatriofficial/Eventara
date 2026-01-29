package com.eventara;

import com.eventara.metrics.config.MetricsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.eventara")
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(MetricsProperties.class)
public class EventaraApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventaraApplication.class, args);
    }
}
