package com.soen345.project.service;

import com.soen345.project.dto.AdminEventRequest;
import com.soen345.project.dto.EventDto;
import com.soen345.project.model.Customer;
import com.soen345.project.model.Event;
import com.soen345.project.model.Reservation;
import com.soen345.project.repository.CategoryRepository;
import com.soen345.project.repository.EventRepository;
import com.soen345.project.repository.LocationRepository;
import com.soen345.project.repository.ReservationRepository;
import com.soen345.project.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminEventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    public AdminEventService(EventRepository eventRepository,
                              CategoryRepository categoryRepository,
                              LocationRepository locationRepository,
                              ReservationRepository reservationRepository,
                              UserRepository userRepository,
                              EmailService emailService,
                              SmsService smsService) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.locationRepository = locationRepository;
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    @Transactional
    public EventDto createEvent(Long adminId, AdminEventRequest request) {
        validateCategoryAndLocation(request);

        Event event = new Event();
        applyRequest(event, request);
        event.setCreatedBy(adminId);
        event.setStatus(Event.EventStatus.ACTIVE);

        Event saved = eventRepository.save(event);
        return convertToDto(saved);
    }

    @Transactional
    public EventDto updateEvent(Long eventId, AdminEventRequest request) {
        Event event = findActiveOrThrow(eventId);
        validateCategoryAndLocation(request);
        applyRequest(event, request);
        Event saved = eventRepository.save(event);
        return convertToDto(saved);
    }

    @Transactional
    public EventDto cancelEvent(Long eventId) {
        Event event = findActiveOrThrow(eventId);

        event.setStatus(Event.EventStatus.CANCELLED);
        Event saved = eventRepository.save(event);

        List<Reservation> activeReservations = reservationRepository.findByEventId(eventId)
                .stream()
                .filter(r -> r.getStatus() == Reservation.ReservationStatus.RESERVED)
                .collect(Collectors.toList());

        for (Reservation reservation : activeReservations) {
            reservation.setStatus(Reservation.ReservationStatus.CANCELLED);
            reservationRepository.save(reservation);
            notifyCustomerOfCancellation(reservation, event);
        }

        return convertToDto(saved);
    }


    public List<EventDto> getAllEvents() {
        return eventRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public EventDto getEventById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        return convertToDto(event);
    }


    private Event findActiveOrThrow(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        if (event.getStatus() == Event.EventStatus.CANCELLED) {
            throw new RuntimeException("Event is already cancelled");
        }
        return event;
    }

    private void validateCategoryAndLocation(AdminEventRequest request) {
        categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new RuntimeException("Location not found"));
    }

    private void applyRequest(Event event, AdminEventRequest request) {
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setEventDate(request.getEventDate());
        event.setCategoryId(request.getCategoryId());
        event.setLocationId(request.getLocationId());
        event.setTotalTickets(request.getTotalTickets());
    }

    private void notifyCustomerOfCancellation(Reservation reservation, Event event) {
        userRepository.findById(reservation.getUserId()).ifPresent(user -> {
            String locationDetails = event.getLocationId() != null
                    ? locationRepository.findById(event.getLocationId())
                            .map(loc -> loc.getVenueName() + ", " + loc.getCity())
                            .orElse("TBD")
                    : "TBD";

            String subject = "Eventigo – Event Cancelled: " + event.getTitle();
            String body = "Dear " + user.getFirstName() + ",\n\n"
                    + "We regret to inform you that the following event has been cancelled:\n\n"
                    + "Event          : " + event.getTitle() + "\n"
                    + "Date           : " + event.getEventDate() + "\n"
                    + "Location       : " + locationDetails + "\n"
                    + "Reservation ID : " + reservation.getId() + "\n"
                    + "Tickets        : " + reservation.getQuantity() + "\n\n"
                    + "Your reservation has been automatically cancelled. "
                    + "If you made a payment, a refund will be processed shortly.\n\n"
                    + "We apologise for the inconvenience.\n"
                    + "The Eventigo Team";

            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                try {
                    emailService.sendVerificationEmail(user.getEmail(), subject, body);
                } catch (Exception ignored) { }
            } else if (user instanceof Customer customer
                    && customer.getPhoneNumber() != null
                    && !customer.getPhoneNumber().isBlank()) {
                try {
                    smsService.sendVerificationSms(
                            customer.getPhoneNumber(),
                            "Eventigo: Event '" + event.getTitle()
                                    + "' (Booking #" + reservation.getId() + ") has been cancelled.");
                } catch (Exception ignored) { }
            }
        });
    }

    private EventDto convertToDto(Event event) {
        EventDto dto = new EventDto();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setEventDate(event.getEventDate());
        dto.setTotalTickets(event.getTotalTickets());
        dto.setStatus(event.getStatus());

        if (event.getCategoryId() != null) {
            categoryRepository.findById(event.getCategoryId())
                    .ifPresent(cat -> dto.setCategoryName(cat.getName()));
        }

        if (event.getLocationId() != null) {
            locationRepository.findById(event.getLocationId())
                    .ifPresent(loc -> {
                        dto.setLocationName(loc.getVenueName());
                        dto.setCity(loc.getCity());
                    });
        }

        return dto;
    }
}
