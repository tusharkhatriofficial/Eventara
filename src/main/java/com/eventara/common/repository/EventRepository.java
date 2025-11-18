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

}
