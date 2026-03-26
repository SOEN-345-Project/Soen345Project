package com.soen345.project.dto;

import com.soen345.project.model.Event;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime eventDate;
    private String categoryName;
    private String locationName;
    private String city;
    private Integer totalTickets;
    private Event.EventStatus status;
}