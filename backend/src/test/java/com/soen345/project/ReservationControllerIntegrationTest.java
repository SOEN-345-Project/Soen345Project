package com.soen345.project;

import com.soen345.project.controller.ReservationController;
import com.soen345.project.dto.ReservationRequest;
import com.soen345.project.dto.ReservationResponse;
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
class ReservationControllerIntegrationTest {

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
    private ReservationController reservationController;

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

    private Customer currentUser;
    private Event activeEvent;

    @BeforeEach
    void setup() {
        reservationRepository.deleteAll();
        eventRepository.deleteAll();
        categoryRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();

        Category category = categoryRepository.save(new Category(null, "Concert"));
        Location location = locationRepository.save(new Location(null, "Bell Centre", "Montreal", "1909 Ave des Canadiens"));

        Customer user = new Customer();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.reservation@example.com");
        user.setPassword("encoded");
        user.setEnabled(true);
        user.setPhoneNumber("5551112222");
        currentUser = (Customer) userRepository.save(user);

        Event event = new Event();
        event.setTitle("Jazz Night");
        event.setDescription("Live jazz show");
        event.setCategoryId(category.getId());
        event.setLocationId(location.getId());
        event.setEventDate(LocalDateTime.now().plusDays(5));
        event.setTotalTickets(100);
        event.setCreatedBy(currentUser.getId());
        event.setStatus(Event.EventStatus.ACTIVE);
        activeEvent = eventRepository.save(event);
    }

    @Test
    void reserve_createsReservationAndReturnsDetails() {
        ReservationRequest request = new ReservationRequest();
        request.setEventId(activeEvent.getId());
        request.setQuantity(2);

        ResponseEntity<?> response = reservationController.reserve(currentUser, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(ReservationResponse.class);
        ReservationResponse body = (ReservationResponse) response.getBody();
        assertThat(body.getEventId()).isEqualTo(activeEvent.getId());
        assertThat(body.getQuantity()).isEqualTo(2);
        assertThat(body.getStatus()).isEqualTo(Reservation.ReservationStatus.RESERVED);
        assertThat(body.getEventTitle()).isEqualTo("Jazz Night");
    }

    @Test
    void reserve_withUnknownEvent_returnsBadRequest() {
        ReservationRequest request = new ReservationRequest();
        request.setEventId(999999L);
        request.setQuantity(1);

        ResponseEntity<?> response = reservationController.reserve(currentUser, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(String.valueOf(response.getBody())).contains("Event not found");
    }

    @Test
    void cancel_existingReservation_marksReservationCancelled() {
        ReservationRequest request = new ReservationRequest();
        request.setEventId(activeEvent.getId());
        request.setQuantity(1);
        ResponseEntity<?> reserveResponse = reservationController.reserve(currentUser, request);
        Long reservationId = ((ReservationResponse) reserveResponse.getBody()).getReservationId();

        ResponseEntity<?> cancelResponse = reservationController.cancel(currentUser, reservationId);

        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelResponse.getBody()).isInstanceOf(ReservationResponse.class);
        ReservationResponse body = (ReservationResponse) cancelResponse.getBody();
        assertThat(body.getStatus()).isEqualTo(Reservation.ReservationStatus.CANCELLED);
    }

    @Test
    void cancel_withUnknownReservation_returnsBadRequest() {
        ResponseEntity<?> response = reservationController.cancel(currentUser, 424242L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(String.valueOf(response.getBody())).contains("Reservation not found");
    }

    @Test
    void myReservations_returnsOnlyCurrentUserReservations() {
        Reservation mine = new Reservation();
        mine.setUserId(currentUser.getId());
        mine.setEventId(activeEvent.getId());
        mine.setQuantity(3);
        mine.setStatus(Reservation.ReservationStatus.RESERVED);
        reservationRepository.save(mine);

        Customer other = new Customer();
        other.setFirstName("Other");
        other.setLastName("User");
        other.setEmail("other.reservation@example.com");
        other.setPassword("encoded");
        other.setEnabled(true);
        other.setPhoneNumber("5553334444");
        Customer otherSaved = (Customer) userRepository.save(other);

        Reservation theirs = new Reservation();
        theirs.setUserId(otherSaved.getId());
        theirs.setEventId(activeEvent.getId());
        theirs.setQuantity(1);
        theirs.setStatus(Reservation.ReservationStatus.RESERVED);
        reservationRepository.save(theirs);

        ResponseEntity<List<ReservationResponse>> response = reservationController.myReservations(currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().getQuantity()).isEqualTo(3);
    }
}
