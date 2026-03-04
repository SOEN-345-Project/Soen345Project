package com.soen345.project.service;

import com.soen345.project.dto.EventDto;
import com.soen345.project.model.Event;
import com.soen345.project.repository.CategoryRepository;
import com.soen345.project.repository.EventRepository;
import com.soen345.project.repository.LocationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;

    public EventService(EventRepository eventRepository,
                        CategoryRepository categoryRepository,
                        LocationRepository locationRepository) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.locationRepository = locationRepository;
    }


    public List<EventDto> getAllActiveEvents() {
        List<Event> events = eventRepository.findActiveEvents(LocalDateTime.now());
        return events.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }


    public List<EventDto> searchEvents(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllActiveEvents();
        }
        List<Event> events = eventRepository.searchEventsByKeyword(keyword.trim(), LocalDateTime.now());
        return events.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<EventDto> filterEvents(Long categoryId, Long locationId,
                                       LocalDateTime startDate, LocalDateTime endDate) {
        List<Event> events;

        if (startDate != null && endDate != null) {
            events = eventRepository.findByDateRange(startDate, endDate);
        } else if (categoryId != null) {
            events = eventRepository.findByCategory(categoryId, LocalDateTime.now());
        } else if (locationId != null) {
            events = eventRepository.findByLocation(locationId, LocalDateTime.now());
        } else {
            events = eventRepository.findActiveEvents(LocalDateTime.now());
        }

        // Apply additional filters if needed
        if (categoryId != null && (startDate != null || locationId != null)) {
            events = events.stream()
                    .filter(e -> e.getCategoryId().equals(categoryId))
                    .collect(Collectors.toList());
        }

        if (locationId != null && (startDate != null || categoryId != null)) {
            events = events.stream()
                    .filter(e -> e.getLocationId().equals(locationId))
                    .collect(Collectors.toList());
        }

        return events.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }


    private EventDto convertToDto(Event event) {
        EventDto dto = new EventDto();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setEventDate(event.getEventDate());
        dto.setTotalTickets(event.getTotalTickets());

        // Fetch category name
        if (event.getCategoryId() != null) {
            categoryRepository.findById(event.getCategoryId())
                    .ifPresent(cat -> dto.setCategoryName(cat.getName()));
        }

        // Fetch location details
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
