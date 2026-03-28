package com.soen345.project.controller;

import com.soen345.project.dto.AdminEventRequest;
import com.soen345.project.dto.EventDto;
import com.soen345.project.model.User;
import com.soen345.project.service.AdminEventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/events")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEventController {

    private final AdminEventService adminEventService;

    public AdminEventController(AdminEventService adminEventService) {
        this.adminEventService = adminEventService;
    }


    // POST /api/admin/events
    @PostMapping
    public ResponseEntity<?> createEvent(@AuthenticationPrincipal User currentUser,
                                          @Valid @RequestBody AdminEventRequest request) {
        try {
            EventDto created = adminEventService.createEvent(currentUser.getId(), request);
            return ResponseEntity.ok(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // PUT /api/admin/events/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable Long id,
                                          @Valid @RequestBody AdminEventRequest request) {
        try {
            EventDto updated = adminEventService.updateEvent(id, request);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DELETE /api/admin/events/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelEvent(@PathVariable Long id) {
        try {
            EventDto cancelled = adminEventService.cancelEvent(id);
            return ResponseEntity.ok(cancelled);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // GET /api/admin/events
    @GetMapping
    public ResponseEntity<List<EventDto>> getAllEvents() {
        return ResponseEntity.ok(adminEventService.getAllEvents());
    }

    // GET /api/admin/events/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getEvent(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminEventService.getEventById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
