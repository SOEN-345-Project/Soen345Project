package com.soen345.project.service;

import com.soen345.project.dto.ReservationRequest;
import com.soen345.project.dto.ReservationResponse;
import com.soen345.project.model.Customer;
import com.soen345.project.model.Event;
import com.soen345.project.model.Reservation;
import com.soen345.project.model.User;
import com.soen345.project.repository.EventRepository;
import com.soen345.project.repository.LocationRepository;
import com.soen345.project.repository.ReservationRepository;
import com.soen345.project.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    public ReservationService(ReservationRepository reservationRepository,
                               EventRepository eventRepository,
                               LocationRepository locationRepository,
                               UserRepository userRepository,
                               EmailService emailService,
                               SmsService smsService) {
        this.reservationRepository = reservationRepository;
        this.eventRepository = eventRepository;
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    // Reserve tickets for an event
    @Transactional
    public synchronized ReservationResponse reserveTickets(Long userId, ReservationRequest request) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (event.getTotalTickets() == null) {
            throw new RuntimeException("Event ticket information is unavailable");
        }

        Integer bookedTickets = reservationRepository.sumActiveTicketsByEventId(event.getId());
        if (bookedTickets == null) bookedTickets = 0;

        int remaining = event.getTotalTickets() - bookedTickets;

        if (remaining <= 0) {
            throw new RuntimeException("Tickets Sold Out");
        }

        if (request.getQuantity() > remaining) {
            throw new RuntimeException("Not enough tickets available. Only " + remaining + " left.");
        }

        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setEventId(event.getId());
        reservation.setQuantity(request.getQuantity());
        reservation.setStatus(Reservation.ReservationStatus.ACTIVE);

        Reservation saved = reservationRepository.save(reservation);

        sendConfirmationNotification(userId, saved, event);

        return buildResponse(saved, event);
    }


     // Cancel an active reservation
    @Transactional
    public ReservationResponse cancelReservation(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findByIdAndUserId(reservationId, userId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        if (reservation.getStatus() == Reservation.ReservationStatus.CANCELLED) {
            throw new RuntimeException("Reservation is already cancelled");
        }

        reservation.setStatus(Reservation.ReservationStatus.CANCELLED);
        Reservation updated = reservationRepository.save(reservation);

        Event event = eventRepository.findById(reservation.getEventId()).orElse(null);
        return buildResponse(updated, event);
    }

    // Get all reservations for the current user.
    public List<ReservationResponse> getUserReservations(Long userId) {
        return reservationRepository.findByUserId(userId).stream()
                .map(r -> {
                    Event event = eventRepository.findById(r.getEventId()).orElse(null);
                    return buildResponse(r, event);
                })
                .collect(Collectors.toList());
    }

    private void sendConfirmationNotification(Long userId, Reservation reservation, Event event) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        String locationDetails = "";
        if (event.getLocationId() != null) {
            locationDetails = locationRepository.findById(event.getLocationId())
                    .map(loc -> loc.getVenueName() + ", " + loc.getCity())
                    .orElse("TBD");
        }

        String message = buildConfirmationMessage(user.getFirstName(), reservation, event, locationDetails);

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            try {
                emailService.sendVerificationEmail(
                        user.getEmail(),
                        "Eventigo – Booking Confirmation #" + reservation.getId(),
                        message);
            } catch (Exception e) {
                // Log but don't fail the reservation
            }
        } else if (user instanceof Customer customer
                && customer.getPhoneNumber() != null
                && !customer.getPhoneNumber().isBlank()) {
            try {
                smsService.sendVerificationSms(customer.getPhoneNumber(),
                        "Eventigo: Booking #" + reservation.getId() + " confirmed for "
                                + event.getTitle() + " x" + reservation.getQuantity());
            } catch (Exception e) {
                // Log but don't fail the reservation
            }
        }
    }

    private String buildConfirmationMessage(String firstName, Reservation reservation,
                                             Event event, String location) {
        return "Dear " + firstName + ",\n\n"
                + "Your booking is confirmed!\n\n"
                + "Reservation ID : " + reservation.getId() + "\n"
                + "Event          : " + event.getTitle() + "\n"
                + "Date           : " + event.getEventDate() + "\n"
                + "Location       : " + location + "\n"
                + "Tickets        : " + reservation.getQuantity() + "\n\n"
                + "Thank you for using Eventigo!\n"
                + "The Eventigo Team";
    }

    private ReservationResponse buildResponse(Reservation reservation, Event event) {
        ReservationResponse response = new ReservationResponse();
        response.setReservationId(reservation.getId());
        response.setEventId(reservation.getEventId());
        response.setQuantity(reservation.getQuantity());
        response.setStatus(reservation.getStatus());
        response.setCreatedAt(reservation.getCreatedAt());

        if (event != null) {
            response.setEventTitle(event.getTitle());
            response.setEventDate(event.getEventDate());

            if (event.getLocationId() != null) {
                locationRepository.findById(event.getLocationId())
                        .ifPresent(loc -> response.setEventLocation(loc.getVenueName() + ", " + loc.getCity()));
            }
        }

        return response;
    }
}
