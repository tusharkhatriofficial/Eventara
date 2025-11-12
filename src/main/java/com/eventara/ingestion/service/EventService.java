package com.eventara.ingestion.service;
import com.eventara.ingestion.mapper.EventMapper;
import com.eventara.ingestion.model.dto.EventRequest;
import com.eventara.ingestion.model.dto.EventResponse;
import com.eventara.ingestion.model.entity.Event;
import com.eventara.ingestion.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    @Autowired
    private EventMapper eventMapper;

    @Autowired
    private EventRepository eventRepository;

    public EventResponse processEvent(EventRequest request){
        logger.info("Processing event: {}", request.getEventType());

        //Convert DTO to entity
        Event entity = eventMapper.toEntity(request);
        logger.debug("Converted to entity with eventId: {}", entity.getEventId());

        //Save in DB
        Event saved = eventRepository.save(entity);
        logger.info("Saved event to database: {}", saved.getEventId());

        //convert entity to dto
        return  eventMapper.toResponse(saved);
    }


    /**
     * Get event by ID (for later use)
     */
    public Event getEventById(String eventId) {
        return eventRepository.findByEventId(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));
    }

    /**
     * Get recent events (useful for dashboard - Module 2)
     */
    public List<Event> getRecentEvents(int limit) {
        return eventRepository.findRecentEvents(limit);
    }


}

