package com.soen345.project;

import com.soen345.project.dto.ReservationRequest;
import com.soen345.project.dto.ReservationResponse;
import com.soen345.project.model.Customer;
import com.soen345.project.model.Event;
import com.soen345.project.model.Location;
import com.soen345.project.model.Reservation;
import com.soen345.project.repository.EventRepository;
import com.soen345.project.repository.LocationRepository;
import com.soen345.project.repository.ReservationRepository;
import com.soen345.project.repository.UserRepository;
import com.soen345.project.service.EmailService;
import com.soen345.project.service.ReservationService;
import com.soen345.project.service.SmsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private SmsService smsService;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void reserveTickets_savesReservationAndReturnsResponse() {
        Event event = new Event();
        event.setId(10L);
        event.setTitle("Concert");
        event.setEventDate(LocalDateTime.now().plusDays(1));
        event.setTotalTickets(100);
        event.setLocationId(5L);

        ReservationRequest request = new ReservationRequest();
        request.setEventId(10L);
        request.setQuantity(2);

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(reservationRepository.sumActiveTicketsByEventId(10L)).thenReturn(10);

        Reservation saved = new Reservation();
        saved.setId(99L);
        saved.setUserId(1L);
        saved.setEventId(10L);
        saved.setQuantity(2);
        saved.setStatus(Reservation.ReservationStatus.RESERVED);
        saved.setCreatedAt(LocalDateTime.now());
        when(reservationRepository.save(any(Reservation.class))).thenReturn(saved);

        Location loc = new Location();
        loc.setVenueName("Hall");
        loc.setCity("Montreal");
        when(locationRepository.findById(5L)).thenReturn(Optional.of(loc));

        Customer user = new Customer();
        user.setId(1L);
        user.setEmail("c@example.com");
        user.setFirstName("Pat");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ReservationResponse response = reservationService.reserveTickets(1L, request);

        assertThat(response.getReservationId()).isEqualTo(99L);
        assertThat(response.getQuantity()).isEqualTo(2);
        assertThat(response.getEventTitle()).isEqualTo("Concert");
        assertThat(response.getEventLocation()).isEqualTo("Hall, Montreal");

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getStatus()).isEqualTo(Reservation.ReservationStatus.RESERVED);
    }

    @Test
    void reserveTickets_throwsWhenEventMissing() {
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());
        ReservationRequest request = new ReservationRequest();
        request.setEventId(1L);
        request.setQuantity(1);

        assertThatThrownBy(() -> reservationService.reserveTickets(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Event not found");
    }

    @Test
    void reserveTickets_throwsWhenTotalTicketsNull() {
        Event event = new Event();
        event.setId(1L);
        event.setTotalTickets(null);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        ReservationRequest request = new ReservationRequest();
        request.setEventId(1L);
        request.setQuantity(1);

        assertThatThrownBy(() -> reservationService.reserveTickets(1L, request))
                .hasMessageContaining("ticket information is unavailable");
    }

    @Test
    void reserveTickets_throwsWhenSoldOut() {
        Event event = new Event();
        event.setId(1L);
        event.setTotalTickets(10);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(reservationRepository.sumActiveTicketsByEventId(1L)).thenReturn(10);
        ReservationRequest request = new ReservationRequest();
        request.setEventId(1L);
        request.setQuantity(1);

        assertThatThrownBy(() -> reservationService.reserveTickets(1L, request))
                .hasMessageContaining("Sold Out");
    }

    @Test
    void reserveTickets_throwsWhenNotEnoughTickets() {
        Event event = new Event();
        event.setId(1L);
        event.setTotalTickets(10);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(reservationRepository.sumActiveTicketsByEventId(1L)).thenReturn(8);
        ReservationRequest request = new ReservationRequest();
        request.setEventId(1L);
        request.setQuantity(5);

        assertThatThrownBy(() -> reservationService.reserveTickets(1L, request))
                .hasMessageContaining("Not enough tickets");
    }

    @Test
    void cancelReservation_cancelsWhenActive() {
        Reservation reservation = new Reservation();
        reservation.setId(7L);
        reservation.setUserId(2L);
        reservation.setEventId(3L);
        reservation.setQuantity(1);
        reservation.setStatus(Reservation.ReservationStatus.RESERVED);
        reservation.setCreatedAt(LocalDateTime.now());

        when(reservationRepository.findByIdAndUserId(7L, 2L)).thenReturn(Optional.of(reservation));

        Reservation updated = new Reservation();
        updated.setId(7L);
        updated.setUserId(2L);
        updated.setEventId(3L);
        updated.setQuantity(1);
        updated.setStatus(Reservation.ReservationStatus.CANCELLED);
        updated.setCreatedAt(reservation.getCreatedAt());
        when(reservationRepository.save(reservation)).thenReturn(updated);

        Event event = new Event();
        event.setId(3L);
        event.setTitle("Show");
        event.setEventDate(LocalDateTime.now());
        when(eventRepository.findById(3L)).thenReturn(Optional.of(event));

        ReservationResponse response = reservationService.cancelReservation(2L, 7L);

        assertThat(response.getStatus()).isEqualTo(Reservation.ReservationStatus.CANCELLED);
    }

    @Test
    void cancelReservation_throwsWhenAlreadyCancelled() {
        Reservation reservation = new Reservation();
        reservation.setStatus(Reservation.ReservationStatus.CANCELLED);
        when(reservationRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancelReservation(1L, 1L))
                .hasMessageContaining("already cancelled");
    }

    @Test
    void getUserReservations_mapsEvents() {
        Reservation r = new Reservation();
        r.setId(1L);
        r.setUserId(5L);
        r.setEventId(20L);
        r.setQuantity(2);
        r.setStatus(Reservation.ReservationStatus.RESERVED);
        r.setCreatedAt(LocalDateTime.now());

        when(reservationRepository.findByUserId(5L)).thenReturn(List.of(r));

        Event event = new Event();
        event.setId(20L);
        event.setTitle("Gig");
        event.setEventDate(LocalDateTime.now());
        when(eventRepository.findById(20L)).thenReturn(Optional.of(event));

        List<ReservationResponse> list = reservationService.getUserReservations(5L);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getEventTitle()).isEqualTo("Gig");
    }
}
