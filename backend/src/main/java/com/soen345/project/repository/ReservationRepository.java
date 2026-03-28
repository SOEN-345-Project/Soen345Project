package com.soen345.project.repository;

import com.soen345.project.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserId(Long userId);

    List<Reservation> findByEventId(Long eventId);

    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM Reservation r " +
           "WHERE r.eventId = :eventId AND r.status = 'ACTIVE'")
    Integer sumActiveTicketsByEventId(@Param("eventId") Long eventId);

    Optional<Reservation> findByIdAndUserId(Long id, Long userId);
}
