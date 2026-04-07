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
import static org.mockito.Mockito.never;
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
    void searchEvents_nullKeyword_delegatesToActiveEvents() {
        when(eventRepository.findActiveEvents(any(LocalDateTime.class))).thenReturn(List.of());

        eventService.searchEvents(null);

        verify(eventRepository).findActiveEvents(any(LocalDateTime.class));
        verify(eventRepository, never()).searchEventsByKeyword(any(), any());
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

    @Test
    void filterEvents_dateRangePlusCategory_filtersPostQuery() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(2);
        Event match = new Event();
        match.setId(1L);
        match.setTitle("A");
        match.setDescription("d");
        match.setEventDate(start.plusHours(1));
        match.setCategoryId(5L);
        match.setLocationId(9L);
        Event other = new Event();
        other.setId(2L);
        other.setTitle("B");
        other.setDescription("d");
        other.setEventDate(start.plusHours(2));
        other.setCategoryId(6L);
        other.setLocationId(9L);
        when(eventRepository.findByDateRange(start, end)).thenReturn(List.of(match, other));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(new Category(5L, "Jazz")));
        when(locationRepository.findById(9L)).thenReturn(Optional.of(location("Hall", "Mtl")));

        var dtos = eventService.filterEvents(5L, null, start, end);

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).getTitle()).isEqualTo("A");
    }

    @Test
    void filterEvents_dateRangePlusLocation_filtersPostQuery() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(1);
        Event match = new Event();
        match.setId(1L);
        match.setTitle("Here");
        match.setDescription("d");
        match.setEventDate(start);
        match.setCategoryId(1L);
        match.setLocationId(20L);
        Event other = new Event();
        other.setId(2L);
        other.setTitle("There");
        other.setDescription("d");
        other.setEventDate(start);
        other.setCategoryId(1L);
        other.setLocationId(21L);
        when(eventRepository.findByDateRange(start, end)).thenReturn(List.of(match, other));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(new Category(1L, "C")));
        when(locationRepository.findById(20L)).thenReturn(Optional.of(location("V", "City")));

        var dtos = eventService.filterEvents(null, 20L, start, end);

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).getTitle()).isEqualTo("Here");
    }

    @Test
    void filterEvents_categoryAndLocationNoDate_usesFindByCategoryThenLocationPostFilter() {
        Event wrongLoc = new Event();
        wrongLoc.setId(1L);
        wrongLoc.setTitle("Wrong");
        wrongLoc.setDescription("d");
        wrongLoc.setEventDate(LocalDateTime.now());
        wrongLoc.setCategoryId(7L);
        wrongLoc.setLocationId(100L);
        Event match = new Event();
        match.setId(2L);
        match.setTitle("Right");
        match.setDescription("d");
        match.setEventDate(LocalDateTime.now());
        match.setCategoryId(7L);
        match.setLocationId(200L);
        when(eventRepository.findByCategory(eq(7L), any(LocalDateTime.class)))
                .thenReturn(List.of(wrongLoc, match));
        when(categoryRepository.findById(7L)).thenReturn(Optional.of(new Category(7L, "Rock")));
        when(locationRepository.findById(200L)).thenReturn(Optional.of(location("Stadium", "Mtl")));

        var dtos = eventService.filterEvents(7L, 200L, null, null);

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).getTitle()).isEqualTo("Right");
        verify(eventRepository).findByCategory(eq(7L), any(LocalDateTime.class));
        verify(eventRepository, never()).findByLocation(any(), any());
        verify(eventRepository, never()).findActiveEvents(any());
    }

    @Test
    void filterEvents_categoryLocationAndDateRange_postFiltersBoth() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(3);
        Event wrongCat = new Event();
        wrongCat.setId(1L);
        wrongCat.setTitle("BadCat");
        wrongCat.setDescription("d");
        wrongCat.setEventDate(start.plusHours(1));
        wrongCat.setCategoryId(1L);
        wrongCat.setLocationId(50L);
        Event wrongLoc = new Event();
        wrongLoc.setId(2L);
        wrongLoc.setTitle("BadLoc");
        wrongLoc.setDescription("d");
        wrongLoc.setEventDate(start.plusHours(2));
        wrongLoc.setCategoryId(2L);
        wrongLoc.setLocationId(99L);
        Event match = new Event();
        match.setId(3L);
        match.setTitle("Good");
        match.setDescription("d");
        match.setEventDate(start.plusHours(3));
        match.setCategoryId(2L);
        match.setLocationId(50L);
        when(eventRepository.findByDateRange(start, end))
                .thenReturn(List.of(wrongCat, wrongLoc, match));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(new Category(2L, "Pop")));
        when(locationRepository.findById(50L)).thenReturn(Optional.of(location("Club", "Qc")));

        var dtos = eventService.filterEvents(2L, 50L, start, end);

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).getTitle()).isEqualTo("Good");
        verify(eventRepository).findByDateRange(start, end);
    }

    @Test
    void filterEvents_noFilters_usesActiveEvents() {
        when(eventRepository.findActiveEvents(any(LocalDateTime.class))).thenReturn(List.of());

        assertThat(eventService.filterEvents(null, null, null, null)).isEmpty();
        verify(eventRepository).findActiveEvents(any(LocalDateTime.class));
    }

    @Test
    void convertToDto_skipsOptionalCategoryWhenMissing() {
        Event event = new Event();
        event.setId(1L);
        event.setTitle("T");
        event.setDescription("D");
        event.setEventDate(LocalDateTime.now());
        event.setCategoryId(99L);
        event.setLocationId(null);
        when(eventRepository.findActiveEvents(any(LocalDateTime.class))).thenReturn(List.of(event));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        var dtos = eventService.getAllActiveEvents();

        assertThat(dtos.get(0).getCategoryName()).isNull();
    }

    @Test
    void getAllActiveEvents_nullLocationId_omitsLocationNameAndCity() {
        LocalDateTime now = LocalDateTime.now();
        Event event = new Event();
        event.setId(5L);
        event.setTitle("NoVenue");
        event.setDescription("D");
        event.setEventDate(now);
        event.setCategoryId(11L);
        event.setLocationId(null);
        event.setTotalTickets(100);

        when(eventRepository.findActiveEvents(any(LocalDateTime.class))).thenReturn(List.of(event));
        when(categoryRepository.findById(11L)).thenReturn(Optional.of(new Category(11L, "Arts")));

        List<EventDto> dtos = eventService.getAllActiveEvents();

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).getLocationName()).isNull();
        assertThat(dtos.get(0).getCity()).isNull();
    }

    private static Location location(String venue, String city) {
        Location l = new Location();
        l.setVenueName(venue);
        l.setCity(city);
        return l;
    }
}
