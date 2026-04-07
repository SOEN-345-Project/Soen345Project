package com.soen345.project.service;

import com.soen345.project.dto.AdminEventRequest;
import com.soen345.project.dto.EventDto;
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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminEventServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private SmsService smsService;

    @InjectMocks
    private AdminEventService adminEventService;

    @Test
    void createEvent_validatesAndSaves() {
        AdminEventRequest req = request();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(new Category(1L, "Cat")));
        when(locationRepository.findById(2L)).thenReturn(Optional.of(loc()));

        Event savedEntity = new Event();
        savedEntity.setId(50L);
        savedEntity.setTitle(req.getTitle());
        savedEntity.setDescription(req.getDescription());
        savedEntity.setEventDate(req.getEventDate());
        savedEntity.setCategoryId(1L);
        savedEntity.setLocationId(2L);
        savedEntity.setTotalTickets(100);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEntity);

        EventDto dto = adminEventService.createEvent(99L, req);

        assertThat(dto.getId()).isEqualTo(50L);
        assertThat(dto.getTitle()).isEqualTo("Title");
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void createEvent_throwsWhenCategoryMissing() {
        AdminEventRequest req = request();
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminEventService.createEvent(1L, req))
                .hasMessageContaining("Category not found");
    }

    @Test
    void updateEvent_throwsWhenCancelled() {
        Event existing = new Event();
        existing.setId(1L);
        existing.setStatus(Event.EventStatus.CANCELLED);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> adminEventService.updateEvent(1L, request()))
                .hasMessageContaining("already cancelled");
    }

    @Test
    void cancelEvent_cancelsReservations() {
        Event event = new Event();
        event.setId(3L);
        event.setTitle("Party");
        event.setStatus(Event.EventStatus.ACTIVE);
        event.setLocationId(2L);
        event.setEventDate(LocalDateTime.now());
        when(eventRepository.findById(3L)).thenReturn(Optional.of(event));
        when(locationRepository.findById(2L)).thenReturn(Optional.of(loc()));

        Reservation active = new Reservation();
        active.setId(10L);
        active.setUserId(5L);
        active.setEventId(3L);
        active.setStatus(Reservation.ReservationStatus.RESERVED);
        when(reservationRepository.findByEventId(3L)).thenReturn(List.of(active));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventDto dto = adminEventService.cancelEvent(3L);

        assertThat(dto).isNotNull();
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void getEventById_throwsWhenMissing() {
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminEventService.getEventById(1L))
                .hasMessageContaining("Event not found");
    }

    @Test
    void getEventById_returnsDto() {
        Event event = new Event();
        event.setId(7L);
        event.setTitle("Gig");
        event.setDescription("D");
        event.setEventDate(LocalDateTime.now());
        event.setCategoryId(1L);
        event.setLocationId(2L);
        event.setTotalTickets(10);
        event.setStatus(Event.EventStatus.ACTIVE);
        when(eventRepository.findById(7L)).thenReturn(Optional.of(event));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(new Category(1L, "Music")));
        when(locationRepository.findById(2L)).thenReturn(Optional.of(loc()));

        EventDto dto = adminEventService.getEventById(7L);

        assertThat(dto.getTitle()).isEqualTo("Gig");
        assertThat(dto.getCategoryName()).isEqualTo("Music");
    }

    @Test
    void getAllEvents_mapsAll() {
        Event event = new Event();
        event.setId(1L);
        event.setTitle("T");
        event.setDescription("D");
        event.setEventDate(LocalDateTime.now());
        event.setCategoryId(1L);
        event.setLocationId(2L);
        event.setTotalTickets(5);
        event.setStatus(Event.EventStatus.ACTIVE);
        when(eventRepository.findAll()).thenReturn(List.of(event));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(new Category(1L, "Cat")));
        when(locationRepository.findById(2L)).thenReturn(Optional.of(loc()));

        assertThat(adminEventService.getAllEvents()).hasSize(1);
    }

    @Test
    void updateEvent_appliesChangesWhenActive() {
        Event existing = new Event();
        existing.setId(4L);
        existing.setStatus(Event.EventStatus.ACTIVE);
        when(eventRepository.findById(4L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(new Category(1L, "Cat")));
        when(locationRepository.findById(2L)).thenReturn(Optional.of(loc()));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventDto dto = adminEventService.updateEvent(4L, request());

        assertThat(dto.getTitle()).isEqualTo("Title");
        verify(eventRepository).save(existing);
    }

    @Test
    void createEvent_throwsWhenLocationMissing() {
        AdminEventRequest req = request();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(new Category(1L, "Cat")));
        when(locationRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminEventService.createEvent(1L, req))
                .hasMessageContaining("Location not found");
    }

    @Test
    void cancelEvent_notifiesCustomerByEmail() throws Exception {
        Event event = new Event();
        event.setId(3L);
        event.setTitle("Party");
        event.setStatus(Event.EventStatus.ACTIVE);
        event.setLocationId(2L);
        event.setEventDate(LocalDateTime.now());
        when(eventRepository.findById(3L)).thenReturn(Optional.of(event));
        when(locationRepository.findById(2L)).thenReturn(Optional.of(loc()));

        Reservation active = new Reservation();
        active.setId(10L);
        active.setUserId(5L);
        active.setEventId(3L);
        active.setQuantity(2);
        active.setStatus(Reservation.ReservationStatus.RESERVED);
        when(reservationRepository.findByEventId(3L)).thenReturn(List.of(active));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        Customer cust = new Customer();
        cust.setFirstName("Sam");
        cust.setEmail("sam@example.com");
        when(userRepository.findById(5L)).thenReturn(Optional.of(cust));

        adminEventService.cancelEvent(3L);

        verify(emailService).sendVerificationEmail(eq("sam@example.com"), any(), contains("cancelled"));
    }

    @Test
    void cancelEvent_notifiesCustomerBySmsWhenNoEmail() throws Exception {
        Event event = new Event();
        event.setId(3L);
        event.setTitle("Show");
        event.setStatus(Event.EventStatus.ACTIVE);
        event.setLocationId(null);
        event.setEventDate(LocalDateTime.now());
        when(eventRepository.findById(3L)).thenReturn(Optional.of(event));

        Reservation active = new Reservation();
        active.setId(11L);
        active.setUserId(6L);
        active.setEventId(3L);
        active.setQuantity(1);
        active.setStatus(Reservation.ReservationStatus.RESERVED);
        when(reservationRepository.findByEventId(3L)).thenReturn(List.of(active));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        Customer cust = new Customer();
        cust.setFirstName("Mo");
        cust.setEmail(" ");
        cust.setPhoneNumber("+1777");
        when(userRepository.findById(6L)).thenReturn(Optional.of(cust));

        adminEventService.cancelEvent(3L);

        verify(smsService).sendVerificationSms(eq("+1777"), contains("cancelled"));
    }

    @Test
    void cancelEvent_ignoresAlreadyCancelledReservations() {
        Event event = new Event();
        event.setId(40L);
        event.setTitle("Mix");
        event.setStatus(Event.EventStatus.ACTIVE);
        event.setLocationId(2L);
        event.setEventDate(LocalDateTime.now());
        when(eventRepository.findById(40L)).thenReturn(Optional.of(event));
        when(locationRepository.findById(2L)).thenReturn(Optional.of(loc()));

        Reservation wasCancelled = new Reservation();
        wasCancelled.setId(1L);
        wasCancelled.setUserId(100L);
        wasCancelled.setEventId(40L);
        wasCancelled.setStatus(Reservation.ReservationStatus.CANCELLED);

        Reservation active = new Reservation();
        active.setId(2L);
        active.setUserId(101L);
        active.setEventId(40L);
        active.setStatus(Reservation.ReservationStatus.RESERVED);

        when(reservationRepository.findByEventId(40L)).thenReturn(List.of(wasCancelled, active));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        adminEventService.cancelEvent(40L);

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(2L);
        assertThat(captor.getValue().getStatus()).isEqualTo(Reservation.ReservationStatus.CANCELLED);
    }

    @Test
    void cancelEvent_skipsNotificationWhenUserHasNoEmailNorPhone() throws Exception {
        Event event = new Event();
        event.setId(41L);
        event.setTitle("Quiet");
        event.setStatus(Event.EventStatus.ACTIVE);
        event.setLocationId(null);
        event.setEventDate(LocalDateTime.now());
        when(eventRepository.findById(41L)).thenReturn(Optional.of(event));

        Reservation active = new Reservation();
        active.setId(50L);
        active.setUserId(200L);
        active.setEventId(41L);
        active.setStatus(Reservation.ReservationStatus.RESERVED);
        when(reservationRepository.findByEventId(41L)).thenReturn(List.of(active));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        Customer silent = new Customer();
        silent.setFirstName("X");
        silent.setEmail(null);
        silent.setPhoneNumber(null);
        when(userRepository.findById(200L)).thenReturn(Optional.of(silent));

        adminEventService.cancelEvent(41L);

        verify(emailService, never()).sendVerificationEmail(any(), any(), any());
        verify(smsService, never()).sendVerificationSms(any(), any());
    }

    private static AdminEventRequest request() {
        AdminEventRequest r = new AdminEventRequest();
        r.setTitle("Title");
        r.setDescription("Desc");
        r.setEventDate(LocalDateTime.now().plusDays(1));
        r.setCategoryId(1L);
        r.setLocationId(2L);
        r.setTotalTickets(100);
        return r;
    }

    private static Location loc() {
        Location l = new Location();
        l.setId(2L);
        l.setVenueName("V");
        l.setCity("C");
        return l;
    }
}
