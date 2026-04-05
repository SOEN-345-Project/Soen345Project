package com.soen345.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.soen345.project.dto.AdminEventRequest;
import com.soen345.project.dto.EventDto;
import com.soen345.project.model.Administrator;
import com.soen345.project.service.AdminEventService;
import com.soen345.project.service.JwtService;
import com.soen345.project.testsupport.MvcSecuritySupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminEventController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean
    private AdminEventService adminEventService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @AfterEach
    void tearDown() {
        MvcSecuritySupport.clearSecurityContext();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createEvent_returnsCreated() throws Exception {
        Administrator admin = adminUser(11L);
        AdminEventRequest req = new AdminEventRequest();
        req.setTitle("T");
        req.setEventDate(LocalDateTime.now().plusDays(1));
        req.setCategoryId(1L);
        req.setLocationId(2L);
        req.setTotalTickets(50);

        EventDto dto = new EventDto();
        dto.setId(100L);
        dto.setTitle("T");
        when(adminEventService.createEvent(eq(11L), any(AdminEventRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/admin/events")
                        .with(MvcSecuritySupport.principalAsUser(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllEvents_returnsList() throws Exception {
        when(adminEventService.getAllEvents()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/events"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cancelEvent_badRequest_returnsMessage() throws Exception {
        when(adminEventService.cancelEvent(5L)).thenThrow(new RuntimeException("Event not found"));

        mockMvc.perform(delete("/api/admin/events/5"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Event not found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateEvent_returnsDto() throws Exception {
        AdminEventRequest req = new AdminEventRequest();
        req.setTitle("Updated");
        req.setEventDate(LocalDateTime.now().plusDays(2));
        req.setCategoryId(1L);
        req.setLocationId(2L);
        req.setTotalTickets(20);

        EventDto dto = new EventDto();
        dto.setId(3L);
        dto.setTitle("Updated");
        when(adminEventService.updateEvent(eq(3L), any(AdminEventRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/api/admin/events/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));
    }

    private static Administrator adminUser(long id) {
        Administrator a = new Administrator();
        a.setId(id);
        a.setEmail("admin" + id + "@test.com");
        a.setPassword("p");
        a.setFirstName("A");
        a.setLastName("B");
        a.setEnabled(true);
        return a;
    }
}
