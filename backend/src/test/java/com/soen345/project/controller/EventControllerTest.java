package com.soen345.project.controller;

import com.soen345.project.dto.EventDto;
import com.soen345.project.service.EventService;
import com.soen345.project.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventController.class)
@AutoConfigureMockMvc(addFilters = false)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void getAllEvents_returnsList() throws Exception {
        EventDto dto = new EventDto();
        dto.setId(1L);
        dto.setTitle("Concert");
        dto.setEventDate(LocalDateTime.now());
        when(eventService.getAllActiveEvents()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Concert"));
    }

    @Test
    void searchEvents_passesKeyword() throws Exception {
        when(eventService.searchEvents("jazz")).thenReturn(List.of());

        mockMvc.perform(get("/api/events/search").param("keyword", "jazz"))
                .andExpect(status().isOk());
    }
}
