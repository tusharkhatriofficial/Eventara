package com.eventara.ingestion.service;
import com.eventara.ingestion.kafka.EventProducer;
import com.eventara.ingestion.mapper.EventMapper;
import com.eventara.common.dto.EventDto;
import com.eventara.common.dto.EventRequest;
import com.eventara.common.dto.EventResponse;
import com.eventara.ingestion.model.entity.Event;
import com.eventara.common.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;


@Component
public class EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    @Autowired
    private EventMapper eventMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventProducer eventProducer;  // ← NEW: Kafka Producer

    public EventResponse processEvent(EventRequest request){
        logger.info("Processing event: {}", request.getEventType());

        //Convert DTO to entity
        Event entity = eventMapper.toEntity(request);
        logger.debug("Converted to entity with eventId: {}", entity.getEventId());

        //Save in DB
//        Event saved = eventRepository.save(entity);
//        logger.info("Saved event to database: {}", saved.getEventId());

        //Send to Kafka (asynchronous - returns immediately!)
        eventProducer.sendEvent(entity);

        //creating response
        EventResponse response = EventResponse.accepted(
                entity.getEventId(),
                entity.getEventType()
        );

        logger.info("Event accepted and queued to Kafka: eventId={}", entity.getEventId());

        //convert entity to dto
        return response;
    }

    //Get paginated events
    public Page<EventDto> getEvents(int page, int size){
        logger.info("Fetching events: page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Event> events = eventRepository.findAllByOrderByTimestampDesc(pageable);

        return events.map(eventMapper::toDto);
    }

    //Get paginated events by type
    public Page<EventDto> getEventsByType(String eventType, int page, int size) {
        logger.info("Fetching events by type: eventType={}, page={}, size={}",
                eventType, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Event> events = eventRepository.findByEventTypeOrderByTimestampDesc(
                eventType, pageable
        );

        Page<EventDto> res = events.map(eventMapper::toDto);

        return res;
    }

    //Get single event by eventId
    public EventDto getEventById(String eventId) {
        logger.info("Fetching event by id: eventId={}", eventId);

        Event event = eventRepository.findByEventId(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));

        return eventMapper.toDto(event);
    }


    /**
     * Get event by ID (for later use)
     */
//    public Event getEventById(String eventId) {
//        return eventRepository.findByEventId(eventId)
//                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));
//    }

    /**
     * Get recent events (useful for dashboard - Module 2)
     */
//    public List<Event> getRecentEvents(int limit) {
//        return eventRepository.findRecentEvents(limit);
//    }


}

