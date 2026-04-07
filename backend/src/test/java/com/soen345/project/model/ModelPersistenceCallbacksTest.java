package com.soen345.project.model;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ModelPersistenceCallbacksTest {

    @Test
    void eventOnCreate_setsCreatedAtAndDefaultStatus() throws Exception {
        Event event = new Event();
        event.setTitle("T");
        event.setEventDate(LocalDateTime.now());
        invoke(event, "onCreate");

        assertThat(event.getCreatedAt()).isNotNull();
        assertThat(event.getStatus()).isEqualTo(Event.EventStatus.ACTIVE);
    }

    @Test
    void eventOnCreate_setsStatusWhenNull() throws Exception {
        Event event = new Event();
        event.setTitle("T");
        event.setEventDate(LocalDateTime.now());
        ReflectionTestUtils.setField(event, "status", null);
        invoke(event, "onCreate");

        assertThat(event.getStatus()).isEqualTo(Event.EventStatus.ACTIVE);
    }

    @Test
    void eventOnCreate_keepsExplicitStatus() throws Exception {
        Event event = new Event();
        event.setTitle("T");
        event.setEventDate(LocalDateTime.now());
        event.setStatus(Event.EventStatus.CANCELLED);
        invoke(event, "onCreate");

        assertThat(event.getStatus()).isEqualTo(Event.EventStatus.CANCELLED);
    }

    @Test
    void reservationOnCreate_setsCreatedAtAndDefaultStatus() throws Exception {
        Reservation reservation = new Reservation();
        invoke(reservation, "onCreate");

        assertThat(reservation.getCreatedAt()).isNotNull();
        assertThat(reservation.getStatus()).isEqualTo(Reservation.ReservationStatus.RESERVED);
    }

    @Test
    void reservationOnCreate_setsStatusWhenNull() throws Exception {
        Reservation reservation = new Reservation();
        ReflectionTestUtils.setField(reservation, "status", null);
        invoke(reservation, "onCreate");

        assertThat(reservation.getStatus()).isEqualTo(Reservation.ReservationStatus.RESERVED);
    }

    private static void invoke(Object target, String method) throws Exception {
        Method m = target.getClass().getDeclaredMethod(method);
        m.setAccessible(true);
        m.invoke(target);
    }
}
