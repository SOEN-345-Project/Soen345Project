package com.soen345.project.controller;

import com.soen345.project.model.Customer;
import com.soen345.project.service.JwtService;
import com.soen345.project.testsupport.MvcSecuritySupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @AfterEach
    void tearDown() {
        MvcSecuritySupport.clearSecurityContext();
    }

    @Test
    void me_withoutUser_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Not authenticated"));
    }

    @Test
    void me_withUser_returnsProfile() throws Exception {
        Customer c = new Customer();
        c.setId(8L);
        c.setFirstName("Jane");
        c.setLastName("Doe");
        c.setEmail("jane@example.com");
        c.setPassword("x");
        c.setEnabled(true);

        mockMvc.perform(get("/api/users/me").with(MvcSecuritySupport.principalAsUser(c)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.role").value("ROLE_CUSTOMER"));
    }

    @Test
    void me_withUser_nullEmail_returnsEmptyEmail() throws Exception {
        Customer c = new Customer();
        c.setId(9L);
        c.setFirstName("No");
        c.setLastName("Mail");
        c.setEmail(null);
        c.setPassword("x");
        c.setEnabled(true);

        mockMvc.perform(get("/api/users/me").with(MvcSecuritySupport.principalAsUser(c)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(""));
    }
}
