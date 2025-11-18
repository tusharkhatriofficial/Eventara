package com.eventara.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public class EventResponse {

    private String eventId;
    private String eventType;
    private String status; //example values: "accepted", "processing", "failed"

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant receivedAt;

    private String message;

    public EventResponse(){}

    public EventResponse(String eventId, String eventType, String status){
        this.eventId = eventId;
        this.eventType = eventType;
        this.status = status;
        this.receivedAt = Instant.now();
    }

    //Builder patter for clean construction
    public static EventResponse accepted(String eventId, String eventType){
        return new EventResponse(eventId, eventType, "accepted");
    }

    public static EventResponse failed(String eventType, String message){
        EventResponse response = new EventResponse();
        response.setEventType(eventType);
        response.setMessage(message);
        response.setStatus("failed");
        response.setReceivedAt(Instant.now());
        return response;
    }

    //getters n setters

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
