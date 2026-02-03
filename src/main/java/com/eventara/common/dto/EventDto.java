package com.eventara.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class EventDto {

    private String eventId;
    private String eventType;
    private String source;
    private String userId;
    private String severity;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant timestamp;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant receivedAt;

    private Map<String, String> tags = new HashMap<>();
    private Map<String, Object> metadata = new HashMap<>();

    public EventDto() {
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    // Calculate processing latency in milliseconds
    @JsonIgnore
    public long getProcessingLatencyMs() {
        // Check metadata first for explicit application latency
        if (metadata != null && metadata.containsKey("latency")) {
            try {
                Object val = metadata.get("latency");
                if (val instanceof Number)
                    return ((Number) val).longValue();
                if (val instanceof String)
                    return Long.parseLong((String) val);
            } catch (Exception ignored) {
                // Ignore parse errors
            }
        }

        if (timestamp != null && receivedAt != null) {
            return Duration.between(timestamp, receivedAt).toMillis();
        }
        return 0;
    }

    @JsonIgnore
    public boolean isError() {
        return "ERROR".equalsIgnoreCase(severity) || "CRITICAL".equalsIgnoreCase(severity);
    }

    @JsonIgnore
    public String getTag(String key) {
        return tags != null ? tags.get(key) : null;
    }

}
