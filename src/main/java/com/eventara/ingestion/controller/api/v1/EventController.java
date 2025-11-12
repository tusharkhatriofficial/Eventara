package com.eventara.ingestion.controller.api.v1;
import com.eventara.ingestion.model.dto.EventRequest;
import com.eventara.ingestion.model.dto.EventResponse;
import com.eventara.ingestion.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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


}
