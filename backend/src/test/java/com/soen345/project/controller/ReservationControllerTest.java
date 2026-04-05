package com.soen345.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.soen345.project.dto.ReservationRequest;
import com.soen345.project.dto.ReservationResponse;
import com.soen345.project.model.Customer;
import com.soen345.project.model.Reservation;
import com.soen345.project.service.JwtService;
import com.soen345.project.service.ReservationService;
import com.soen345.project.testsupport.MvcSecuritySupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReservationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @AfterEach
    void tearDown() {
        MvcSecuritySupport.clearSecurityContext();
    }

    @Test
    void reserve_returnsResponse() throws Exception {
        Customer principal = customer(5L);

        ReservationRequest request = new ReservationRequest();
        request.setEventId(10L);
        request.setQuantity(2);

        ReservationResponse response = new ReservationResponse();
        response.setReservationId(99L);
        response.setEventId(10L);
        response.setQuantity(2);
        response.setStatus(Reservation.ReservationStatus.RESERVED);
        response.setCreatedAt(LocalDateTime.now());

        when(reservationService.reserveTickets(eq(5L), any(ReservationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/reservations")
                        .with(MvcSecuritySupport.principalAsUser(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(99));
    }

    @Test
    void reserve_badRequest_returnsMessage() throws Exception {
        Customer principal = customer(1L);
        ReservationRequest request = new ReservationRequest();
        request.setEventId(1L);
        request.setQuantity(1);
        when(reservationService.reserveTickets(eq(1L), any(ReservationRequest.class)))
                .thenThrow(new RuntimeException("Tickets Sold Out"));

        mockMvc.perform(post("/api/reservations")
                        .with(MvcSecuritySupport.principalAsUser(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Tickets Sold Out"));
    }

    @Test
    void myReservations_returnsList() throws Exception {
        Customer principal = customer(3L);
        when(reservationService.getUserReservations(3L)).thenReturn(List.of());

        mockMvc.perform(get("/api/reservations/my").with(MvcSecuritySupport.principalAsUser(principal)))
                .andExpect(status().isOk());
    }

    @Test
    void cancel_delegatesToService() throws Exception {
        Customer principal = customer(2L);
        ReservationResponse response = new ReservationResponse();
        response.setReservationId(7L);
        when(reservationService.cancelReservation(2L, 7L)).thenReturn(response);

        mockMvc.perform(delete("/api/reservations/7").with(MvcSecuritySupport.principalAsUser(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(7));
    }

    private static Customer customer(long id) {
        Customer c = new Customer();
        c.setId(id);
        c.setEmail("u" + id + "@test.com");
        c.setPassword("p");
        c.setFirstName("F");
        c.setLastName("L");
        c.setEnabled(true);
        return c;
    }

}
