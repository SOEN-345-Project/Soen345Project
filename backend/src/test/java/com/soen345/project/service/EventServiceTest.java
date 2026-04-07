package com.soen345.project.service;

import com.soen345.project.dto.EventDto;
import com.soen345.project.model.Category;
import com.soen345.project.model.Event;
import com.soen345.project.model.Location;
import com.soen345.project.repository.CategoryRepository;
import com.soen345.project.repository.EventRepository;
import com.soen345.project.repository.LocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private LocationRepository locationRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    void getAllActiveEvents_mapsToDtos() {
        LocalDateTime now = LocalDateTime.now();
        Event event = new Event();
        event.setId(1L);
        event.setTitle("T");
        event.setDescription("D");
        event.setEventDate(now);
        event.setCategoryId(10L);
        event.setLocationId(20L);
        event.setTotalTickets(50);

        when(eventRepository.findActiveEvents(any(LocalDateTime.class))).thenReturn(List.of(event));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(new Category(10L, "Music")));
        when(locationRepository.findById(20L)).thenReturn(Optional.of(location("Arena", "City")));

        List<EventDto> dtos = eventService.getAllActiveEvents();

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).getTitle()).isEqualTo("T");
        assertThat(dtos.get(0).getCategoryName()).isEqualTo("Music");
        assertThat(dtos.get(0).getLocationName()).isEqualTo("Arena");
        assertThat(dtos.get(0).getCity()).isEqualTo("City");
    }

    @Test
    void searchEvents_blankKeyword_delegatesToActiveEvents() {
        when(eventRepository.findActiveEvents(any(LocalDateTime.class))).thenReturn(List.of());

        eventService.searchEvents("   ");

        verify(eventRepository).findActiveEvents(any(LocalDateTime.class));
    }

    @Test
    void searchEvents_trimsAndSearches() {
        when(eventRepository.searchEventsByKeyword(eq("rock"), any(LocalDateTime.class)))
                .thenReturn(List.of());
        eventService.searchEvents("  rock  ");
        verify(eventRepository).searchEventsByKeyword(eq("rock"), any(LocalDateTime.class));
    }

    @Test
    void filterEvents_withDateRange_usesRepository() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(1);
        when(eventRepository.findByDateRange(start, end)).thenReturn(List.of());

        assertThat(eventService.filterEvents(null, null, start, end)).isEmpty();
        verify(eventRepository).findByDateRange(start, end);
    }

    @Test
    void filterEvents_categoryOnly_usesFindByCategory() {
        when(eventRepository.findByCategory(eq(3L), any(LocalDateTime.class))).thenReturn(List.of());
        eventService.filterEvents(3L, null, null, null);
        verify(eventRepository).findByCategory(eq(3L), any(LocalDateTime.class));
    }

    @Test
    void filterEvents_locationOnly_usesFindByLocation() {
        when(eventRepository.findByLocation(eq(9L), any(LocalDateTime.class))).thenReturn(List.of());
        eventService.filterEvents(null, 9L, null, null);
        verify(eventRepository).findByLocation(eq(9L), any(LocalDateTime.class));
    }

    private static Location location(String venue, String city) {
        Location l = new Location();
        l.setVenueName(venue);
        l.setCity(city);
        return l;
    }
}
