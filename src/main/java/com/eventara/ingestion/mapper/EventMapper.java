package com.eventara.ingestion.mapper;
import com.eventara.common.dto.EventDto;
import com.eventara.common.dto.EventRequest;
import com.eventara.common.dto.EventResponse;
import com.eventara.ingestion.model.entity.Event;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    //convert Event to  Dto ()
    public EventDto toDto(Event entity){
        EventDto eventDto = new EventDto();
        eventDto.setEventId(entity.getEventId());
        eventDto.setEventType(entity.getEventType());
        eventDto.setSource(entity.getSource());
        eventDto.setUserId(entity.getUserId());
        eventDto.setReceivedAt(entity.getReceivedAt());
        eventDto.setTimestamp(entity.getTimestamp());
        eventDto.setTags(entity.getTags());
        eventDto.setMetadata(entity.getMetadata());

        if(entity.getSeverity() != null){
            eventDto.setSeverity(entity.getSeverity().name());
        }

        return  eventDto;
    }


    //converting Request DTO to entity
    public Event toEntity(EventRequest request){
        Event entity = new Event();

        //mapping required field
        entity.setEventType(request.getEventType());
        entity.setSource(request.getSource());
        entity.setTimestamp(request.getTimestamp());

        //mapping optional fields
        entity.setUserId(request.getUserId());
        entity.setSessionId(request.getSessionId());
        entity.setTags(request.getTags());
        entity.setMetadata(request.getMetadata());

        //Handling severity
        if(request.getSeverity() != null){
            try {
                entity.setSeverity(Event.Severity.valueOf(request.getSeverity().toUpperCase()));
            }catch (IllegalArgumentException e){
                entity.setSeverity(Event.Severity.INFO);
            }
        }

        return entity;

    }


    //converting entity to Response DTO
    public EventResponse toResponse(Event entity){
        EventResponse response = new EventResponse();
        response.setEventId(entity.getEventId());
        response.setEventType(entity.getEventType());
        response.setStatus("accepted");
        response.setReceivedAt(entity.getReceivedAt());
        return response;
    }


}
