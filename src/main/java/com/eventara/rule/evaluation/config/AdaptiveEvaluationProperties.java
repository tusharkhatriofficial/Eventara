package com.eventara.rule.evaluation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "eventara.evaluation.adaptive")
public class AdaptiveEvaluationProperties {

    private boolean enabled = false;
    private Thresholds thresholds = new Thresholds();
    private Intervals intervals = new Intervals();

    public static class Thresholds {
        private double idle = 0.1; // < 5 events/min
        private double low = 1.0; // < 50 events/min
        private double medium = 10.0; // < 500 events/min
        private double high = 100.0; // < 5000 events/min

        // Getters and setters
        public double getIdle() {
            return idle;
        }

        public void setIdle(double idle) {
            this.idle = idle;
        }

        public double getLow() {
            return low;
        }

        public void setLow(double low) {
            this.low = low;
        }

        public double getMedium() {
            return medium;
        }

        public void setMedium(double medium) {
            this.medium = medium;
        }

        public double getHigh() {
            return high;
        }

        public void setHigh(double high) {
            this.high = high;
        }
    }

    public static class Intervals {
        private long idleMs = 30000; // 30 seconds
        private long lowMs = 10000; // 10 seconds
        private long mediumMs = 2000; // 2 seconds
        private long highMs = 500; // 500ms
        private long burstMs = 100; // 100ms

        // Getters and setters
        public long getIdleMs() {
            return idleMs;
        }

        public void setIdleMs(long idleMs) {
            this.idleMs = idleMs;
        }

        public long getLowMs() {
            return lowMs;
        }

        public void setLowMs(long lowMs) {
            this.lowMs = lowMs;
        }

        public long getMediumMs() {
            return mediumMs;
        }

        public void setMediumMs(long mediumMs) {
            this.mediumMs = mediumMs;
        }

        public long getHighMs() {
            return highMs;
        }

        public void setHighMs(long highMs) {
            this.highMs = highMs;
        }

        public long getBurstMs() {
            return burstMs;
        }

        public void setBurstMs(long burstMs) {
            this.burstMs = burstMs;
        }
    }

    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Thresholds getThresholds() {
        return thresholds;
    }

    public void setThresholds(Thresholds thresholds) {
        this.thresholds = thresholds;
    }

    public Intervals getIntervals() {
        return intervals;
    }

    public void setIntervals(Intervals intervals) {
        this.intervals = intervals;
    }
}