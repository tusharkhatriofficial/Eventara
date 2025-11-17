package com.eventara.ingestion.kafka;
import com.eventara.ingestion.model.entity.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;

@Service
public class EventProducer {
    public static final Logger logger = LoggerFactory.getLogger(EventProducer.class);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${eventara.kafka.topics.events-raw}")
    private String topicName;


    /**
     * Send event to Kafka asynchronously
     *
     * @param event The event to send
     * @return CompletableFuture with send result
     */

    public CompletableFuture<SendResult<String, Object>> sendEvent(Event event){
        logger.info("Sending event to Kafka topic '{}': eventId={}, eventType={}",
                topicName, event.getEventId(), event.getEventType());

        // Sending to Kafka with eventId as key (for partitioning)
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate
                .send(topicName, event.getEventId(), event);

        //callback for success or failure
        future.whenComplete((result, ex) -> {
            if(ex != null){
                logger.info("Successfully sent event to Kafka: eventId={}, partition={}, offset={}",
                        event.getEventId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }else{
                logger.error("Failed to send event to Kafka: eventId={}, error={}",
                        event.getEventId(), ex.getMessage(), ex);
            }
        });

        return future;
    }


    /**
     * Send event synchronously (blocks until sent)
     * Use when required to ensure message was sent before continuing
     */
    public void sendEventSync(Event event) {
        try {
            logger.info("Sending event synchronously to Kafka: eventId={}", event.getEventId());

            SendResult<String, Object> result = kafkaTemplate
                    .send(topicName, event.getEventId(), event)
                    .get(); // This blocks!

            logger.info("Event sent successfully: partition={}, offset={}",
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

        } catch (Exception e) {
            logger.error("Failed to send event synchronously: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send event to Kafka", e);
        }
    }

}
