package com.soen345.project.service;

import com.soen345.project.model.Customer;
import com.soen345.project.model.User;
import com.soen345.project.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    @Test
    void allUsers_returnsAllFromRepository() {
        Customer u1 = new Customer();
        u1.setId(1L);
        Customer u2 = new Customer();
        u2.setId(2L);
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        List<User> result = userService.allUsers();

        assertThat(result).hasSize(2).containsExactly(u1, u2);
        verify(userRepository).findAll();
    }

    @Test
    void allUsers_returnsEmptyWhenNone() {
        when(userRepository.findAll()).thenReturn(List.of());

        assertThat(userService.allUsers()).isEmpty();
    }
}
