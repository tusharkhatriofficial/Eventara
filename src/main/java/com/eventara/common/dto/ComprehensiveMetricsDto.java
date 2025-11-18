package com.eventara.common.dto;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ComprehensiveMetricsDto {

    private SummaryMetrics summary;
    private ThroughputMetrics throughput;
    private TimeWindowMetrics timeWindows;
    private Map<String, EventTypeMetrics> eventsByType;
    private Map<String, SourceMetrics> eventsBySource;
    private Map<String, Long> eventsBySeverity;
    private UserMetrics userMetrics;
    private TopEventsMetrics topEvents;
    private ErrorAnalysisMetrics errorAnalysis;
    private PerformanceMetrics performance;
    private List<AnomalyAlert> anomalies;

    public ComprehensiveMetricsDto() {
        this.eventsByType = new ConcurrentHashMap<>();
        this.eventsBySource = new ConcurrentHashMap<>();
        this.eventsBySeverity = new ConcurrentHashMap<>();
        this.anomalies = new ArrayList<AnomalyAlert>();
//        this.anomalies = new List<AnomalyAlert>();
    }

    // ===== SUMMARY METRICS =====
    public static class SummaryMetrics {
        private long totalEvents;
        private int uniqueSources;
        private int uniqueEventTypes;
        private int uniqueUsers;
        private String systemHealth; // "healthy", "degraded", "critical"

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        private Instant lastUpdated;

        // Getters and Setters
        public long getTotalEvents() { return totalEvents; }
        public void setTotalEvents(long totalEvents) { this.totalEvents = totalEvents; }

        public int getUniqueSources() { return uniqueSources; }
        public void setUniqueSources(int uniqueSources) { this.uniqueSources = uniqueSources; }

        public int getUniqueEventTypes() { return uniqueEventTypes; }
        public void setUniqueEventTypes(int uniqueEventTypes) { this.uniqueEventTypes = uniqueEventTypes; }

        public int getUniqueUsers() { return uniqueUsers; }
        public void setUniqueUsers(int uniqueUsers) { this.uniqueUsers = uniqueUsers; }

        public String getSystemHealth() { return systemHealth; }
        public void setSystemHealth(String systemHealth) { this.systemHealth = systemHealth; }

        public Instant getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
    }

    // ===== THROUGHPUT METRICS =====
    public static class ThroughputMetrics {
        private CurrentThroughput current;
        private PeakThroughput peak;
        private AverageThroughput average;

        public static class CurrentThroughput {
            private double perSecond;
            private double perMinute;
            private double perHour;
            private double perDay;

            // Getters and Setters
            public double getPerSecond() { return perSecond; }
            public void setPerSecond(double perSecond) { this.perSecond = perSecond; }
            public double getPerMinute() { return perMinute; }
            public void setPerMinute(double perMinute) { this.perMinute = perMinute; }
            public double getPerHour() { return perHour; }
            public void setPerHour(double perHour) { this.perHour = perHour; }
            public double getPerDay() { return perDay; }
            public void setPerDay(double perDay) { this.perDay = perDay; }
        }

        public static class PeakThroughput {
            private double value;
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
            private Instant timestamp;

            // Getters and Setters
            public double getValue() { return value; }
            public void setValue(double value) { this.value = value; }
            public Instant getTimestamp() { return timestamp; }
            public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
        }

        public static class AverageThroughput {
            private double last1Hour;
            private double last24Hours;

            // Getters and Setters
            public double getLast1Hour() { return last1Hour; }
            public void setLast1Hour(double last1Hour) { this.last1Hour = last1Hour; }
            public double getLast24Hours() { return last24Hours; }
            public void setLast24Hours(double last24Hours) { this.last24Hours = last24Hours; }
        }

        // Getters and Setters
        public CurrentThroughput getCurrent() { return current; }
        public void setCurrent(CurrentThroughput current) { this.current = current; }
        public PeakThroughput getPeak() { return peak; }
        public void setPeak(PeakThroughput peak) { this.peak = peak; }
        public AverageThroughput getAverage() { return average; }
        public void setAverage(AverageThroughput average) { this.average = average; }
    }

    // ===== TIME WINDOW METRICS =====
    public static class TimeWindowMetrics {
        private long last1Minute;
        private long last5Minutes;
        private long last15Minutes;
        private long last1Hour;
        private long last24Hours;

        // Getters and Setters
        public long getLast1Minute() { return last1Minute; }
        public void setLast1Minute(long last1Minute) { this.last1Minute = last1Minute; }
        public long getLast5Minutes() { return last5Minutes; }
        public void setLast5Minutes(long last5Minutes) { this.last5Minutes = last5Minutes; }
        public long getLast15Minutes() { return last15Minutes; }
        public void setLast15Minutes(long last15Minutes) { this.last15Minutes = last15Minutes; }
        public long getLast1Hour() { return last1Hour; }
        public void setLast1Hour(long last1Hour) { this.last1Hour = last1Hour; }
        public long getLast24Hours() { return last24Hours; }
        public void setLast24Hours(long last24Hours) { this.last24Hours = last24Hours; }
    }


    // ===== EVENT TYPE METRICS =====
    public static class EventTypeMetrics {
        private long count;
        private double percentage;
        private double avgLatency; // receivedAt - timestamp

        // Getters and Setters
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
        public double getAvgLatency() { return avgLatency; }
        public void setAvgLatency(double avgLatency) { this.avgLatency = avgLatency; }
    }

    // ===== SOURCE METRICS =====
    public static class SourceMetrics {
        private long count;
        private String health; // "healthy", "degraded", "down"
        private double avgLatency;
        private long errorCount;
        private double errorRate;

        // Getters and Setters
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
        public String getHealth() { return health; }
        public void setHealth(String health) { this.health = health; }
        public double getAvgLatency() { return avgLatency; }
        public void setAvgLatency(double avgLatency) { this.avgLatency = avgLatency; }
        public long getErrorCount() { return errorCount; }
        public void setErrorCount(long errorCount) { this.errorCount = errorCount; }
        public double getErrorRate() { return errorRate; }
        public void setErrorRate(double errorRate) { this.errorRate = errorRate; }
    }

    // ===== USER METRICS =====
    public static class UserMetrics {
        private long totalUniqueUsers;
        private long activeUsersLast1Hour;
        private long activeUsersLast24Hours;
        private List<UserActivity> topActiveUsers;

        public static class UserActivity {
            private String userId;
            private long eventCount;

            public UserActivity(String userId, long eventCount) {
                this.userId = userId;
                this.eventCount = eventCount;
            }

            // Getters and Setters
            public String getUserId() { return userId; }
            public void setUserId(String userId) { this.userId = userId; }
            public long getEventCount() { return eventCount; }
            public void setEventCount(long eventCount) { this.eventCount = eventCount; }
        }

        // Getters and Setters
        public long getTotalUniqueUsers() { return totalUniqueUsers; }
        public void setTotalUniqueUsers(long totalUniqueUsers) { this.totalUniqueUsers = totalUniqueUsers; }
        public long getActiveUsersLast1Hour() { return activeUsersLast1Hour; }
        public void setActiveUsersLast1Hour(long activeUsersLast1Hour) { this.activeUsersLast1Hour = activeUsersLast1Hour; }
        public long getActiveUsersLast24Hours() { return activeUsersLast24Hours; }
        public void setActiveUsersLast24Hours(long activeUsersLast24Hours) { this.activeUsersLast24Hours = activeUsersLast24Hours; }
        public List<UserActivity> getTopActiveUsers() { return topActiveUsers; }
        public void setTopActiveUsers(List<UserActivity> topActiveUsers) { this.topActiveUsers = topActiveUsers; }
    }

    // ===== TOP EVENTS METRICS =====
    public static class TopEventsMetrics {
        private List<EventRanking> mostFrequent;
        private List<EventRanking> fastest;
        private List<EventRanking> slowest;

        public static class EventRanking {
            private String type;
            private long count;
            private double avgLatency;

            public EventRanking(String type, long count, double avgLatency) {
                this.type = type;
                this.count = count;
                this.avgLatency = avgLatency;
            }

            // Getters and Setters
            public String getType() { return type; }
            public void setType(String type) { this.type = type; }
            public long getCount() { return count; }
            public void setCount(long count) { this.count = count; }
            public double getAvgLatency() { return avgLatency; }
            public void setAvgLatency(double avgLatency) { this.avgLatency = avgLatency; }
        }

        // Getters and Setters
        public List<EventRanking> getMostFrequent() { return mostFrequent; }
        public void setMostFrequent(List<EventRanking> mostFrequent) { this.mostFrequent = mostFrequent; }
        public List<EventRanking> getFastest() { return fastest; }
        public void setFastest(List<EventRanking> fastest) { this.fastest = fastest; }
        public List<EventRanking> getSlowest() { return slowest; }
        public void setSlowest(List<EventRanking> slowest) { this.slowest = slowest; }
    }

    // ===== ERROR ANALYSIS =====
    public static class ErrorAnalysisMetrics {
        private long totalErrors;
        private double errorRate;
        private List<ErrorBreakdown> errorsByType;
        private List<ErrorBreakdown> errorsBySource;

        public static class ErrorBreakdown {
            private String name;
            private long count;
            private double percentage;

            public ErrorBreakdown(String name, long count, double percentage) {
                this.name = name;
                this.count = count;
                this.percentage = percentage;
            }

            // Getters and Setters
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public long getCount() { return count; }
            public void setCount(long count) { this.count = count; }
            public double getPercentage() { return percentage; }
            public void setPercentage(double percentage) { this.percentage = percentage; }
        }

        // Getters and Setters
        public long getTotalErrors() { return totalErrors; }
        public void setTotalErrors(long totalErrors) { this.totalErrors = totalErrors; }
        public double getErrorRate() { return errorRate; }
        public void setErrorRate(double errorRate) { this.errorRate = errorRate; }
        public List<ErrorBreakdown> getErrorsByType() { return errorsByType; }
        public void setErrorsByType(List<ErrorBreakdown> errorsByType) { this.errorsByType = errorsByType; }
        public List<ErrorBreakdown> getErrorsBySource() { return errorsBySource; }
        public void setErrorsBySource(List<ErrorBreakdown> errorsBySource) { this.errorsBySource = errorsBySource; }
    }

    // ===== PERFORMANCE METRICS =====
    public static class PerformanceMetrics {
        private double avgLatency; // receivedAt - timestamp
        private double p50;
        private double p95;
        private double p99;
        private double maxLatency;
        private double minLatency;

        // Getters and Setters
        public double getAvgLatency() { return avgLatency; }
        public void setAvgLatency(double avgLatency) { this.avgLatency = avgLatency; }
        public double getP50() { return p50; }
        public void setP50(double p50) { this.p50 = p50; }
        public double getP95() { return p95; }
        public void setP95(double p95) { this.p95 = p95; }
        public double getP99() { return p99; }
        public void setP99(double p99) { this.p99 = p99; }
        public double getMaxLatency() { return maxLatency; }
        public void setMaxLatency(double maxLatency) { this.maxLatency = maxLatency; }
        public double getMinLatency() { return minLatency; }
        public void setMinLatency(double minLatency) { this.minLatency = minLatency; }
    }

    // ===== ANOMALY ALERTS =====
    public static class AnomalyAlert {
        private String severity; // "info", "warning", "critical"
        private String type; // "spike", "drop", "high_error_rate", "high_latency"
        private String message;
        private double threshold;
        private double currentValue;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        private Instant detectedAt;

        // Getters and Setters
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public double getThreshold() { return threshold; }
        public void setThreshold(double threshold) { this.threshold = threshold; }
        public double getCurrentValue() { return currentValue; }
        public void setCurrentValue(double currentValue) { this.currentValue = currentValue; }
        public Instant getDetectedAt() { return detectedAt; }
        public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }
    }

    // Main Getters and Setters
    public SummaryMetrics getSummary() { return summary; }
    public void setSummary(SummaryMetrics summary) { this.summary = summary; }

    public ThroughputMetrics getThroughput() { return throughput; }
    public void setThroughput(ThroughputMetrics throughput) { this.throughput = throughput; }

    public TimeWindowMetrics getTimeWindows() { return timeWindows; }
    public void setTimeWindows(TimeWindowMetrics timeWindows) { this.timeWindows = timeWindows; }

    public Map<String, EventTypeMetrics> getEventsByType() { return eventsByType; }
    public void setEventsByType(Map<String, EventTypeMetrics> eventsByType) { this.eventsByType = eventsByType; }

    public Map<String, SourceMetrics> getEventsBySource() { return eventsBySource; }
    public void setEventsBySource(Map<String, SourceMetrics> eventsBySource) { this.eventsBySource = eventsBySource; }

    public Map<String, Long> getEventsBySeverity() { return eventsBySeverity; }
    public void setEventsBySeverity(Map<String, Long> eventsBySeverity) { this.eventsBySeverity = eventsBySeverity; }

    public UserMetrics getUserMetrics() { return userMetrics; }
    public void setUserMetrics(UserMetrics userMetrics) { this.userMetrics = userMetrics; }

    public TopEventsMetrics getTopEvents() { return topEvents; }
    public void setTopEvents(TopEventsMetrics topEvents) { this.topEvents = topEvents; }

    public ErrorAnalysisMetrics getErrorAnalysis() { return errorAnalysis; }
    public void setErrorAnalysis(ErrorAnalysisMetrics errorAnalysis) { this.errorAnalysis = errorAnalysis; }

    public PerformanceMetrics getPerformance() { return performance; }
    public void setPerformance(PerformanceMetrics performance) { this.performance = performance; }

    public List<AnomalyAlert> getAnomalies() { return anomalies; }
    public void setAnomalies(List<AnomalyAlert> anomalies) { this.anomalies = anomalies; }


}

