package com.soen345.project.system_testing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.soen345.project.dto.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.ExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.assertj.RestTestClientResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EventSystemTest {

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
    void testGetAllEvents_returnsResults() {
        String searchKeyword = "Taylor";

        client.get()
                .uri("/api/events")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$[0]").exists();
    }

    @Test
    void testGetAllEvents_returnsNoResults() {
        String searchKeyword = "Taylor";

        client.get()
                .uri("/api/event")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void testSearchEvents_returnsResults() {
        String searchKeyword = "Taylor";

        client.get()
                .uri("/api/events/search?keyword={keyword}", searchKeyword)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].title").isEqualTo("Taylor Swift Night")
                .jsonPath("$[0].description").isEqualTo("Tribute concert");
    }
    @Test
    void testSearchEvents_returnsnoResults() {
        String searchKeyword = "1Taylor";

        client.get()
                .uri("/api/events/search?keyword={keyword}", searchKeyword)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    void testFilterEvents_WithResults() {
        Long categoryId = 1L;
        Long locationId = 1L;
        String startDate = "2025-04-01T09:00:00";
        String endDate = "2026-09-30T18:00:00";

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/events/filter")
                        .queryParam("categoryId", categoryId)
                        .queryParam("locationId", locationId)
                        .queryParam("startDate", startDate)
                        .queryParam("endDate", endDate)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].title").isEqualTo("Coldplay Live")
                .jsonPath("$[0].description").isEqualTo("Coldplay world tour concert");
    }

    @Test
    void testFilterEvents_WithNoResults() {
        Long categoryId = 1L;
        Long locationId = 10L;
        String startDate = "2025-04-01T09:00:00";
        String endDate = "2026-09-30T18:00:00";

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/events/filter")
                        .queryParam("categoryId", categoryId)
                        .queryParam("locationId", locationId)
                        .queryParam("startDate", startDate)
                        .queryParam("endDate", endDate)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(0);
    }



}
