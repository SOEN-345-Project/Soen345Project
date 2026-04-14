package com.soen345.project;

import com.soen345.project.controller.EventController;
import com.soen345.project.dto.EventDto;
import com.soen345.project.model.Category;
import com.soen345.project.model.Event;
import com.soen345.project.model.Location;
import com.soen345.project.repository.CategoryRepository;
import com.soen345.project.repository.EventRepository;
import com.soen345.project.repository.LocationRepository;
import com.soen345.project.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class EventControllerIntegrationTest {

    @Autowired
    private EventController eventController;

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationRepository locationRepository;

    @BeforeEach
    void setupData() {
        eventRepository.deleteAll();
        categoryRepository.deleteAll();
        locationRepository.deleteAll();

        Category concert = categoryRepository.save(new Category(null, "Concert"));
        Category sports = categoryRepository.save(new Category(null, "Sports"));
        Location bellCentre = locationRepository.save(new Location(null, "Bell Centre", "Montreal", "1909 Ave des Canadiens"));
        Location olympic = locationRepository.save(new Location(null, "Olympic Stadium", "Montreal", "4545 Pierre-de Coubertin"));

        eventRepository.save(buildEvent(
                "Jazz Night",
                "Live jazz show",
                concert.getId(),
                bellCentre.getId(),
                LocalDateTime.now().plusDays(3),
                Event.EventStatus.ACTIVE
        ));
        eventRepository.save(buildEvent(
                "Soccer Final",
                "Championship game",
                sports.getId(),
                olympic.getId(),
                LocalDateTime.now().plusDays(10),
                Event.EventStatus.ACTIVE
        ));
        eventRepository.save(buildEvent(
                "Old Cancelled Event",
                "Should not appear",
                concert.getId(),
                bellCentre.getId(),
                LocalDateTime.now().plusDays(5),
                Event.EventStatus.CANCELLED
        ));
        eventRepository.save(buildEvent(
                "Past Event",
                "Already happened",
                concert.getId(),
                bellCentre.getId(),
                LocalDateTime.now().minusDays(2),
                Event.EventStatus.ACTIVE
        ));
    }

    @Test
    void getAllEvents_returnsOnlyActiveUpcomingEvents() {
        List<EventDto> events = eventService.getAllActiveEvents();

        assertThat(events).hasSize(2);
        assertThat(events).extracting(EventDto::getTitle)
                .containsExactlyInAnyOrder("Jazz Night", "Soccer Final");
        assertThat(events).allMatch(e -> e.getCategoryName() != null && e.getLocationName() != null);
    }

    @Test
    void searchEvents_filtersByKeywordCaseInsensitive() {
        List<EventDto> events = eventService.searchEvents("jAzZ");

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getTitle()).isEqualTo("Jazz Night");
    }

    @Test
    void filterEvents_byCategory_returnsOnlyMatchingCategory() {
        Long concertCategoryId = categoryRepository.findAll()
                .stream()
                .filter(c -> "Concert".equals(c.getName()))
                .findFirst()
                .orElseThrow()
                .getId();

        List<EventDto> events = eventService.filterEvents(concertCategoryId, null, null, null);

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getTitle()).isEqualTo("Jazz Night");
        assertThat(events.getFirst().getCategoryName()).isEqualTo("Concert");
    }

    @Test
    void filterEvents_byDateRange_returnsOnlyEventsInRange() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(4);

        List<EventDto> events = eventService.filterEvents(null, null, start, end);

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getTitle()).isEqualTo("Jazz Night");
    }

    @Test
    void controller_getAllEvents_returnsSameAsService() {
        ResponseEntity<List<EventDto>> response = eventController.getAllEvents();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).extracting(EventDto::getTitle)
                .containsExactlyInAnyOrder("Jazz Night", "Soccer Final");
    }

    @Test
    void controller_searchEvents_delegatesToService() {
        ResponseEntity<List<EventDto>> response = eventController.searchEvents("soccer");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().getTitle()).isEqualTo("Soccer Final");
    }

    @Test
    void controller_filterEvents_withCategoryAndLocation() {
        Long concertId = categoryRepository.findAll().stream()
                .filter(c -> "Concert".equals(c.getName()))
                .findFirst().orElseThrow().getId();
        Long bellId = locationRepository.findAll().stream()
                .filter(l -> l.getVenueName() != null && l.getVenueName().contains("Bell"))
                .findFirst().orElseThrow().getId();

        ResponseEntity<List<EventDto>> response = eventController.filterEvents(concertId, bellId, null, null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().getTitle()).isEqualTo("Jazz Night");
    }

    @Test
    void controller_filterEvents_withNullParams_returnsAllActiveUpcoming() {
        ResponseEntity<List<EventDto>> response = eventController.filterEvents(null, null, null, null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(2);
    }

    private Event buildEvent(String title,
                             String description,
                             Long categoryId,
                             Long locationId,
                             LocalDateTime eventDate,
                             Event.EventStatus status) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(description);
        event.setCategoryId(categoryId);
        event.setLocationId(locationId);
        event.setEventDate(eventDate);
        event.setTotalTickets(100);
        event.setCreatedBy(1L);
        event.setStatus(status);
        return event;
    }
}
