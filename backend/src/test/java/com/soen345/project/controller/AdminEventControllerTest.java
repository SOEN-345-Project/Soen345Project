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
    void cancelEvent_returnsCancelledDto() throws Exception {
        EventDto dto = new EventDto();
        dto.setId(8L);
        dto.setTitle("Off");
        when(adminEventService.cancelEvent(8L)).thenReturn(dto);

        mockMvc.perform(delete("/api/admin/events/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.title").value("Off"));
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

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateEvent_badRequest_returnsMessage() throws Exception {
        AdminEventRequest req = new AdminEventRequest();
        req.setTitle("X");
        req.setEventDate(LocalDateTime.now().plusDays(1));
        req.setCategoryId(1L);
        req.setLocationId(2L);
        req.setTotalTickets(10);
        when(adminEventService.updateEvent(eq(9L), any(AdminEventRequest.class)))
                .thenThrow(new RuntimeException("Event is already cancelled"));

        mockMvc.perform(put("/api/admin/events/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Event is already cancelled"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getEvent_byId_returnsDto() throws Exception {
        EventDto dto = new EventDto();
        dto.setId(42L);
        dto.setTitle("Solo");
        dto.setTotalTickets(100);
        when(adminEventService.getEventById(42L)).thenReturn(dto);

        mockMvc.perform(get("/api/admin/events/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.title").value("Solo"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getEvent_notFound_returnsBadRequest() throws Exception {
        when(adminEventService.getEventById(999L)).thenThrow(new RuntimeException("Event not found"));

        mockMvc.perform(get("/api/admin/events/999"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Event not found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createEvent_badRequest_returnsMessage() throws Exception {
        Administrator admin = adminUser(7L);
        AdminEventRequest req = new AdminEventRequest();
        req.setTitle("Bad");
        req.setEventDate(LocalDateTime.now().plusDays(1));
        req.setCategoryId(1L);
        req.setLocationId(2L);
        req.setTotalTickets(5);
        when(adminEventService.createEvent(eq(7L), any(AdminEventRequest.class)))
                .thenThrow(new RuntimeException("Category not found"));

        mockMvc.perform(post("/api/admin/events")
                        .with(MvcSecuritySupport.principalAsUser(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Category not found"));
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
