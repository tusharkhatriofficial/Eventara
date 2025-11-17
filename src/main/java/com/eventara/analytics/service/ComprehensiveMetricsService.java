package com.eventara.analytics.service;

import com.eventara.common.dto.ComprehensiveMetricsDto;
import com.eventara.common.dto.EventDto;
import com.eventara.common.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Collections;

@Service
public class ComprehensiveMetricsService {

    public static final Logger logger = LoggerFactory.getLogger(ComprehensiveMetricsService.class);

    private EventRepository eventRepository;

    //core counters
    private final AtomicLong totalEvents = new AtomicLong(0);

    //Event tracking maps
    private final Map<String, AtomicLong> eventsByType = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> eventsBySource = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> eventsBySeverity = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> eventsByUser = new ConcurrentHashMap<>();

    //latency tracking for performance metrics
    private final Map<String, List<Long>> latenciesByType = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> latenciesBySource = new ConcurrentHashMap<>();
    private final List<Long> allLatencies = Collections.synchronizedList(new ArrayList<>());

    //Error tracking
    private final Map<String, AtomicLong> errorsByType = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> errorsBySource = new ConcurrentHashMap<>();

    //timestamp tracking for time windows
    private final Queue<TimestampedEvent> eventTimestamps = new LinkedList<>();

    // Throughput tracking
    private long lastMetricsTimestamp = System.currentTimeMillis();
    private long lastEventCount = 0;
    private double peakThroughput = 0.0;
    private Instant peakThroughputTimestamp = Instant.now();

    // Helper class to track event details over time
    private static class TimestampedEvent {
        long timestamp;
        String eventType;
        String source;
        String userId;
        String severity;
        long latency;

        TimestampedEvent(long timestamp, String eventType, String source, String userId, String severity, long latency) {
            this.timestamp = timestamp;
            this.eventType = eventType;
            this.source = source;
            this.userId = userId;
            this.severity = severity;
            this.latency = latency;
        }
    }

    public synchronized void recordEvent(EventDto event){
        long now = System.currentTimeMillis();

        //basic counters
        totalEvents.incrementAndGet();

        //count by type
        eventsByType.computeIfAbsent(event.getEventType(), k -> new AtomicLong(0)).incrementAndGet();

        //count by source
        eventsBySource.computeIfAbsent(event.getSource(), k -> new AtomicLong(0)).incrementAndGet();

        //count by severity
        if(event.getSeverity() != null){
            eventsBySeverity.computeIfAbsent(event.getSeverity(), k -> new AtomicLong(0)).incrementAndGet();
        }

        //count by user
        if(event.getUserId() != null){
            eventsByUser.computeIfAbsent(event.getUserId(), k -> new AtomicLong(0)).incrementAndGet();
        }

        //calculate and track latency
        long latency = event.getProcessingLatencyMs();
        if (latency>0){
            allLatencies.add(latency);

            //tracking laterncy by type
            latenciesByType.computeIfAbsent(event.getEventType(), k -> Collections.synchronizedList(new ArrayList<>())).add(latency);

            // Track latency by source
            latenciesBySource.computeIfAbsent(event.getSource(), k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(latency);

            //limiting list size to prevent memmory issues (Stores only last 1000 latencies)
            if(allLatencies.size() > 1000){
                allLatencies.remove(0);
            }
        }
        //previous if statement ends above this line

        //track errors
        if(event.isError()){
            errorsByType.computeIfAbsent(event.getEventType(), k -> new AtomicLong(0)).incrementAndGet();
            errorsBySource.computeIfAbsent(event.getSource(), k -> new AtomicLong(0))
                    .incrementAndGet();
        }
        //previous if statement ends above this line

        // Track timestamp for time windows
        TimestampedEvent timestampedEvent = new TimestampedEvent(
          now,
          event.getEventType(),
          event.getSource(),
          event.getUserId(),
          event.getSeverity(),
                latency
        );
        eventTimestamps.offer(timestampedEvent);

        // Clean old events (older than 24 hours)
        long cutoff = now - (24 * 60 * 60 * 1000);
        while (!eventTimestamps.isEmpty() && eventTimestamps.peek().timestamp < cutoff){
            eventTimestamps.poll();
        }

        logger.debug("Event recorded: type={}, source={}, user={}, latency={}ms",
                event.getEventType(), event.getSource(), event.getUserId(), latency);

    }



}
