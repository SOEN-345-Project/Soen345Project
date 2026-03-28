package com.soen345.project.repository;

import com.soen345.project.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    // Find all events that are in the future (not cancelled)
    @Query("SELECT e FROM Event e WHERE e.status='ACTIVE' AND e.eventDate > :now ORDER BY e.eventDate ASC")
    List<Event> findActiveEvents(@Param("now") LocalDateTime now);

    // Search events by keyword in title or description
    @Query("SELECT e FROM Event e WHERE e.status='ACTIVE' AND e.eventDate > :now AND " +
            "(LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Event> searchEventsByKeyword(@Param("keyword") String keyword, @Param("now") LocalDateTime now);

    // Filter events by category
    @Query("SELECT e FROM Event e WHERE e.status='ACTIVE' AND e.eventDate > :now AND e.categoryId = :categoryId")
    List<Event> findByCategory(@Param("categoryId") Long categoryId, @Param("now") LocalDateTime now);

    // Filter events by location
    @Query("SELECT e FROM Event e WHERE e.status='ACTIVE' AND e.eventDate > :now AND e.locationId = :locationId")
    List<Event> findByLocation(@Param("locationId") Long locationId, @Param("now") LocalDateTime now);

    // Filter events by date range
    @Query("SELECT e FROM Event e WHERE e.status='ACTIVE' AND e.eventDate BETWEEN :startDate AND :endDate")
    List<Event> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);
}
