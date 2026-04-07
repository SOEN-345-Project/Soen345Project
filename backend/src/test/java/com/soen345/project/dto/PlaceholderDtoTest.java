package com.soen345.project.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceholderDtoTest {

    @Test
    void customerDto_instantiates() {
        assertThat(new CustomerDto()).isNotNull();
    }

    @Test
    void adminDto_instantiates() {
        assertThat(new AdminDto()).isNotNull();
    }
}
