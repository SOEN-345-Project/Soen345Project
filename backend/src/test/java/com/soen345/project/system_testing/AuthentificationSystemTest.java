package com.soen345.project.system_testing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.soen345.project.dto.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthentificationSystemTest {

    private RestTestClient client;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setup() {
        client = RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void testBasepath_() {
        client.get()
                .uri("/auth/")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Welcome to Eventigo!");
    }

    @Test
    void testHealthEndpoint_returnsOk() {

        client.get()
                .uri("/auth/test")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Authentication controller is working!");
    }

    @Test
    void testHealthEndpoint_wrongPath_returns404() {

        client.get()
                .uri("/auth/test1")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void testLogin_HappyPath_returns200() {
        LoginUserDto loginUserDto = new LoginUserDto();
        loginUserDto.setEmail("youssefyassa7112@gmail.com");
        loginUserDto.setPassword("Messi1111");

        client.post()
                .uri("/auth/login")
                .body(loginUserDto)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testLogin_wrongPassword_returns404() {
        LoginUserDto loginUserDto = new LoginUserDto();
        loginUserDto.setEmail("youssefyassa7112@gmail.com");
        loginUserDto.setPassword("Messi1112");

        client.post()
                .uri("/auth/login")
                .body(loginUserDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void testLogin_wrongEmail_returns404() {
        LoginUserDto loginUserDto = new LoginUserDto();
        loginUserDto.setEmail("yyoussefyassa7112@gmail.com");
        loginUserDto.setPassword("Messi1111");

        client.post()
                .uri("/auth/login")
                .body(loginUserDto)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
