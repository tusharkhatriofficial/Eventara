package com.eventara.ingestion.kafka;
import com.eventara.ingestion.model.entity.Event;
import com.eventara.ingestion.repository.EventRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@Service
public class EventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /*
     * Listens to Kafka topic and processes events
     * This method runs continuously in background!
     *
     * @param payload The event data from Kafka
     * @param partition Which Kafka partition the message came from
     * @param offset Position of message in partition
     * @param acknowledgment Manual acknowledgment to commit offset
     */

    @KafkaListener(
            topics = "${eventara.kafka.topics.events-raw}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void ConsumeEvent(
//            @Payload Map<String, Object> payload,
            @Payload Event event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ){
        try{

            logger.info("Received message from Kafka: partition={}, offset={}", partition, offset);

//            Event event = objectMapper.convertValue(payload, Event.class);

            logger.info("Processing event: eventId={}, eventType={}, source={}",
                    event.getEventId(), event.getEventType(), event.getSource());

            // Cheking if event already exists (deduplication)
            if(eventRepository.existsByEventId(event.getEventId())){
                logger.warn("Event already exists in database, skipping: eventId={}",
                        event.getEventId());
                acknowledgment.acknowledge();
                return;
            }

            //Saving to db
            Event savedEvent = eventRepository.save(event);

            logger.info("Successfully saved event to database: eventId={}, dbId={}",
                    savedEvent.getEventId(), savedEvent.getId());

            // Manually acknowledge (commit offset) ... message won't be reprocessed
            acknowledgment.acknowledge();

        }catch (Exception e){

            logger.error("Error processing event from Kafka: partition={}, offset={}, error={}",
                    partition, offset, e.getMessage(), e);

            // Don't acknowledge - message will be retried
            // In production, Ill try to send to dead-letter queue after N retries
            throw new RuntimeException("Failed to process event!");

        }
    }


    //Optional: Listen to errors
//    @org.springframework.kafka.annotation.KafkaListener(
//            topics = "${eventara.kafka.topics.events-raw}",
//            groupId = "${spring.kafka.consumer.group-id}-error",
//            containerFactory = "kafkaListenerContainerFactory",
//            errorHandler = "kafkaErrorHandler"
//    )
//    public void handleErrors(Exception exception) {
//        logger.error("Kafka consumer error: {}", exception.getMessage(), exception);
//    }
}
