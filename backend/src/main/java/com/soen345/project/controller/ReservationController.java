package com.soen345.project.controller;

import com.soen345.project.dto.ReservationRequest;
import com.soen345.project.dto.ReservationResponse;
import com.soen345.project.model.User;
import com.soen345.project.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    // POST /api/reservations
    @PostMapping
    public ResponseEntity<?> reserve(@AuthenticationPrincipal User currentUser,
                                     @Valid @RequestBody ReservationRequest request) {
        try {
            ReservationResponse response = reservationService.reserveTickets(currentUser.getId(), request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DELETE /api/reservations/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancel(@AuthenticationPrincipal User currentUser,
                                    @PathVariable Long id) {
        try {
            ReservationResponse response = reservationService.cancelReservation(currentUser.getId(), id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // GET /api/reservations/my
    @GetMapping("/my")
    public ResponseEntity<List<ReservationResponse>> myReservations(
            @AuthenticationPrincipal User currentUser) {
        List<ReservationResponse> reservations = reservationService.getUserReservations(currentUser.getId());
        return ResponseEntity.ok(reservations);
    }
}
