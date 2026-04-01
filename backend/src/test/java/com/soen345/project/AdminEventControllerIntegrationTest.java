package com.soen345.project;

import com.soen345.project.controller.AdminEventController;
import com.soen345.project.dto.AdminEventRequest;
import com.soen345.project.dto.EventDto;
import com.soen345.project.model.Administrator;
import com.soen345.project.model.Category;
import com.soen345.project.model.Customer;
import com.soen345.project.model.Event;
import com.soen345.project.model.Location;
import com.soen345.project.model.Reservation;
import com.soen345.project.repository.CategoryRepository;
import com.soen345.project.repository.EventRepository;
import com.soen345.project.repository.LocationRepository;
import com.soen345.project.repository.ReservationRepository;
import com.soen345.project.repository.UserRepository;
import com.soen345.project.service.EmailService;
import com.soen345.project.service.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AdminEventControllerIntegrationTest {

    @TestConfiguration
    static class TestOverrides {
        @Bean
        @Primary
        EmailService emailService() {
            return Mockito.mock(EmailService.class);
        }

        @Bean
        @Primary
        SmsService smsService() {
            return Mockito.mock(SmsService.class);
        }
    }

    @Autowired
    private AdminEventController adminEventController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationRepository locationRepository;

    private Administrator admin;
    private Category category;
    private Location location;

    @BeforeEach
    void setup() {
        reservationRepository.deleteAll();
        eventRepository.deleteAll();
        categoryRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();

        admin = new Administrator();
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEmail("admin@example.com");
        admin.setPassword("encoded");
        admin.setEnabled(true);
        admin = (Administrator) userRepository.save(admin);

        category = categoryRepository.save(new Category(null, "Concert"));
        location = locationRepository.save(new Location(null, "Bell Centre", "Montreal", "1909 Ave des Canadiens"));
    }

    @Test
    void createEvent_createsActiveEventSuccessfully() {
        ResponseEntity<?> response = adminEventController.createEvent(admin, request("Jazz Night", 150));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(EventDto.class);
        EventDto body = (EventDto) response.getBody();
        assertThat(body.getTitle()).isEqualTo("Jazz Night");
        assertThat(body.getCategoryName()).isEqualTo("Concert");
        assertThat(body.getLocationName()).isEqualTo("Bell Centre");
    }

    @Test
    void updateEvent_updatesExistingEventFields() {
        Long eventId = ((EventDto) adminEventController.createEvent(admin, request("Jazz Night", 150)).getBody()).getId();

        AdminEventRequest update = request("Updated Jazz Night", 200);
        ResponseEntity<?> response = adminEventController.updateEvent(eventId, update);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        EventDto body = (EventDto) response.getBody();
        assertThat(body.getTitle()).isEqualTo("Updated Jazz Night");
        assertThat(body.getTotalTickets()).isEqualTo(200);
    }

    @Test
    void cancelEvent_cancelsEventAndActiveReservations() {
        Long eventId = ((EventDto) adminEventController.createEvent(admin, request("To Cancel", 100)).getBody()).getId();

        Customer customer = new Customer();
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("customer@example.com");
        customer.setPassword("encoded");
        customer.setEnabled(true);
        customer.setPhoneNumber("5551112222");
        customer = (Customer) userRepository.save(customer);

        Reservation reservation = new Reservation();
        reservation.setUserId(customer.getId());
        reservation.setEventId(eventId);
        reservation.setQuantity(2);
        reservation.setStatus(Reservation.ReservationStatus.RESERVED);
        reservation = reservationRepository.save(reservation);

        ResponseEntity<?> response = adminEventController.cancelEvent(eventId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Event savedEvent = eventRepository.findById(eventId).orElseThrow();
        assertThat(savedEvent.getStatus()).isEqualTo(Event.EventStatus.CANCELLED);
        Reservation savedReservation = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(savedReservation.getStatus()).isEqualTo(Reservation.ReservationStatus.CANCELLED);
    }

    @Test
    void getAllEvents_returnsCreatedEvents() {
        adminEventController.createEvent(admin, request("E1", 50));
        adminEventController.createEvent(admin, request("E2", 60));

        ResponseEntity<List<EventDto>> response = adminEventController.getAllEvents();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).extracting(EventDto::getTitle)
                .containsExactlyInAnyOrder("E1", "E2");
    }

    @Test
    void getEvent_withUnknownId_returnsBadRequest() {
        ResponseEntity<?> response = adminEventController.getEvent(999999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(String.valueOf(response.getBody())).contains("Event not found");
    }

    private AdminEventRequest request(String title, int tickets) {
        AdminEventRequest request = new AdminEventRequest();
        request.setTitle(title);
        request.setDescription("Description for " + title);
        request.setEventDate(LocalDateTime.now().plusDays(10));
        request.setCategoryId(category.getId());
        request.setLocationId(location.getId());
        request.setTotalTickets(tickets);
        return request;
    }
}
