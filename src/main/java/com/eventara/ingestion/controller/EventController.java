package com.eventara.ingestion.controller;
import com.eventara.common.dto.EventDto;
import com.eventara.common.dto.EventRequest;
import com.eventara.common.dto.EventResponse;
import com.eventara.ingestion.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Event Ingestion", description = "APIs for ingesting events into eventra")
public class EventController {

    private static final Logger logger = LoggerFactory.getLogger(EventController.class);

    @Autowired
    EventService eventService;

    @PostMapping
    @Operation(
            summary = "Ingest a new event",
            description = "Submit an event to Eventara for processing, storage, and analytics"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Event accepted for processing"),
            @ApiResponse(responseCode = "400", description = "Invalid event data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<EventResponse> ingestEvent(@Valid @RequestBody EventRequest request){
        logger.info("Received event: {} from source: {}", request.getEventType(), request.getSource());

        try{
            //process the event though service layer
            EventResponse response = eventService.processEvent(request);

            logger.info("Successfully processed event: {}", response.getEventId());

            return  ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        }catch (Exception e){
            logger.error("Failed to process event: {}", e.getMessage(), e);
            EventResponse errorResponse = EventResponse.failed(
                    request.getEventType(),
                    "Failed to process event: " + e.getMessage()
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }

    }

    @PostMapping("/batch")
    @Operation(
            summary = "Ingest multiple events",
            description = "Submit a batch of events for processing (Coming in Module 2)"
    )
    public ResponseEntity<String> ingestBatch(@RequestBody String events){
        return  ResponseEntity
                .status(HttpStatus.NOT_IMPLEMENTED)
                .body("Ba/tch ingestion coming in Module 2!");
    }


    @GetMapping
    @Operation(summary = "Get paginated events", description = "Retrieve events with pagination, sorted by timestamp (newest first)")
    public ResponseEntity<Page<EventDto>> getEvents(
            @RequestParam int page,
            @RequestParam int size
    ){
        logger.info("GET /events - page={}, size={}", page, size);

        // Limiting max page size
        if (size > 100) {
            size = 100;
        }

        Page<EventDto> res = eventService.getEvents(page, size);;
        return ResponseEntity.ok(res);
    }

    @GetMapping("/type/{eventType}")
    @Operation(summary = "Get events by type")
    public ResponseEntity<Page<EventDto>> getEventsByType(
            @PathVariable String eventType,
            @RequestParam int page,
            @RequestParam int size
    ){

        // Limiting max page size
        if (size > 100) {
            size = 100;
        }

        Page<EventDto> res = eventService.getEventsByType(eventType, page, size);

        return ResponseEntity.ok(res);
    }

    //get event by id
    @GetMapping("/{eventId}")
    @Operation(summary = "Get event by ID")
    public ResponseEntity<EventDto> getEventById(
            @PathVariable String eventId
    ){
        logger.info("GET /events/{}", eventId);

        try{
            EventDto event = eventService.getEventById(eventId);
            return ResponseEntity.ok(event);
        }catch (RuntimeException e){
            logger.error("Event not found: {}", eventId);
            return ResponseEntity.notFound().build();
        }

    }


    @GetMapping("/test")
    @Operation(summary = "Test endpoint", description = "Verify the API is running")
    public ResponseEntity<Map<String, String>> test() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "running");
        response.put("service", "Eventara Ingestion Service");
        response.put("version", "0.1.0-SNAPSHOT");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check service health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));

        return ResponseEntity.ok(response);
    }


    /**
     * Exception handler for validation errors
     * Catches @Valid annotation failures
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, Object> errors = new HashMap<>();
        errors.put("status", "error");
        errors.put("message", "Validation failed");

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        errors.put("errors", fieldErrors);

        logger.warn("Validation error: {}", fieldErrors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errors);
    }

    /**
     * Generic exception handler
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);

        Map<String, String> error = new HashMap<>();
        error.put("status", "error");
        error.put("message", "An unexpected error occurred");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }



}
