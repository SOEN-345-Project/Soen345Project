package com.soen345.project;

import com.soen345.project.controller.UserController;
import com.soen345.project.model.Customer;
import com.soen345.project.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired
    private UserController userController;

    @Autowired
    private UserRepository userRepository;

    private Customer currentUser;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();

        Customer user = new Customer();
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jane@example.com");
        user.setPassword("encoded");
        user.setEnabled(true);
        user.setPhoneNumber("5551112222");
        currentUser = (Customer) userRepository.save(user);
    }

    @Test
    void getCurrentUser_whenAuthenticated_returnsProfileInfo() {
        ResponseEntity<?> response = userController.getCurrentUser(currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body.get("id")).isEqualTo(currentUser.getId());
        assertThat(body.get("firstName")).isEqualTo("Jane");
        assertThat(body.get("lastName")).isEqualTo("Doe");
        assertThat(body.get("email")).isEqualTo("jane@example.com");
        assertThat(body.get("role")).isEqualTo("ROLE_CUSTOMER");
    }

    @Test
    void getCurrentUser_whenNotAuthenticated_returnsUnauthorized() {
        ResponseEntity<?> response = userController.getCurrentUser(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(String.valueOf(response.getBody())).contains("Not authenticated");
    }
}
