package com.soen345.project.dto;

import com.soen345.project.model.Reservation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {

    private Long reservationId;
    private Long eventId;
    private String eventTitle;
    private LocalDateTime eventDate;
    private String eventLocation;
    private Integer quantity;
    private Reservation.ReservationStatus status;
    private LocalDateTime createdAt;
}
