package com.eventara.common.repository;
import org.springframework.data.domain.Pageable;
import com.eventara.ingestion.model.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    //Find event by eventId (our custom UUID, not database ID)
    Optional<Event> findByEventId(String eventId);

    //Find events by type
    //Useful for Module 2 (Analytics)
    List<Event> findByEventType(String eventType);

     //Find events by userId
     //Useful for user-specific queries
    List<Event> findByUserId(String userId);

    /**
     * Find events by source
     * Useful to see which service is sending what
     */
    List<Event> findBySource(String source);

    /**
     * Find events in a time range
     * Useful for analytics and reporting (Module 2)
     */
    List<Event> findByTimestampBetween(Instant startTime, Instant endTime);

    //Pagenated queries
   Page<Event> findAllByOrderByTimestampDesc(Pageable pageable);

   Page<Event> findByEventTypeOrderByTimestampDesc(String eventType, Pageable pageable);

   Page<Event> findBySourceOrderByTimestampDesc(String source, Pageable pageable);


    /**
     * Count events by type (for analytics)
     */
    long countByEventType(String eventType);

    /**
     * Find recent events (last N events)
     * Using native query for better performance
     */
    @Query(value = "SELECT * FROM events ORDER BY timestamp DESC LIMIT :limit",
            nativeQuery = true)
    List<Event> findRecentEvents(@Param("limit") int limit);

    //Check if event already exists (deduplication)
    boolean existsByEventId(String eventId);

    // ===== METRICS CALCULATION QUERIES FOR DROOLS RULE EVALUATION =====

    // Count events in time window
    long countByTimestampBetween(Instant startTime, Instant endTime);

    // Count errors (by severity) in time window
    long countBySeverityAndTimestampBetween(Event.Severity severity, Instant startTime, Instant endTime);

    // Count unique sources
    @Query("SELECT COUNT(DISTINCT e.source) FROM Event e WHERE e.timestamp BETWEEN :startTime AND :endTime")
    int countDistinctSourceByTimestampBetween(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    // Count unique event types
    @Query("SELECT COUNT(DISTINCT e.eventType) FROM Event e WHERE e.timestamp BETWEEN :startTime AND :endTime")
    int countDistinctEventTypeByTimestampBetween(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    // Count unique users
    @Query("SELECT COUNT(DISTINCT e.userId) FROM Event e WHERE e.userId IS NOT NULL AND e.timestamp BETWEEN :startTime AND :endTime")
    int countDistinctUserIdByTimestampBetween(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    // Get distinct sources
    @Query("SELECT DISTINCT e.source FROM Event e WHERE e.timestamp BETWEEN :startTime AND :endTime")
    List<String> findDistinctSourcesByTimestampBetween(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    // Count events by source in time window
    long countBySourceAndTimestampBetween(String source, Instant startTime, Instant endTime);

    // Count errors by source in time window
    long countBySourceAndSeverityAndTimestampBetween(String source, Event.Severity severity, Instant startTime, Instant endTime);

    // Average processing latency (receivedAt - timestamp) in milliseconds
    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (received_at - timestamp)) * 1000) FROM events WHERE timestamp BETWEEN :startTime AND :endTime", nativeQuery = true)
    Double findAvgLatencyBetween(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    // Percentile latency queries (native SQL for TimescaleDB)
    @Query(value = "SELECT PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (received_at - timestamp)) * 1000) FROM events WHERE timestamp BETWEEN :startTime AND :endTime", nativeQuery = true)
    Double findP50LatencyBetween(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    @Query(value = "SELECT PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (received_at - timestamp)) * 1000) FROM events WHERE timestamp BETWEEN :startTime AND :endTime", nativeQuery = true)
    Double findP95LatencyBetween(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    @Query(value = "SELECT PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (received_at - timestamp)) * 1000) FROM events WHERE timestamp BETWEEN :startTime AND :endTime", nativeQuery = true)
    Double findP99LatencyBetween(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    // Min/Max latency
    @Query(value = "SELECT MIN(EXTRACT(EPOCH FROM (received_at - timestamp)) * 1000) FROM events WHERE timestamp BETWEEN :startTime AND :endTime", nativeQuery = true)
    Double findMinLatencyBetween(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    @Query(value = "SELECT MAX(EXTRACT(EPOCH FROM (received_at - timestamp)) * 1000) FROM events WHERE timestamp BETWEEN :startTime AND :endTime", nativeQuery = true)
    Double findMaxLatencyBetween(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    // Average latency by source
    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (received_at - timestamp)) * 1000) FROM events WHERE source = :source AND timestamp BETWEEN :startTime AND :endTime", nativeQuery = true)
    Double findAvgLatencyBySourceBetween(@Param("source") String source, @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

}
