package com.soen345.project.service;

import com.soen345.project.dto.LoginUserDto;
import com.soen345.project.dto.RegisterUserDto;
import com.soen345.project.dto.VerifyUserDto;
import com.soen345.project.model.Administrator;
import com.soen345.project.model.Customer;
import com.soen345.project.model.User;
import com.soen345.project.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private EmailService emailService;
    @Mock
    private SmsService smsService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void signup_email_savesCustomerAndSendsEmail() throws Exception {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setFirstName("A");
        dto.setLastName("B");
        dto.setEmail("a@b.com");
        dto.setPassword("raw");
        dto.setVerificationMethod("EMAIL");

        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("raw")).thenReturn("hashed");
        Customer saved = new Customer();
        saved.setId(1L);
        saved.setEmail("a@b.com");
        saved.setVerificationCode("123456");
        when(userRepository.save(any(Customer.class))).thenReturn(saved);

        User result = authenticationService.signup(dto);

        assertThat(result.getId()).isEqualTo(1L);
        verify(emailService).sendVerificationEmail(eq("a@b.com"), any(), any());
        verify(smsService, never()).sendVerificationSms(any(), any());
    }

    @Test
    void signup_phone_requiresPhoneNumber() {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setVerificationMethod("PHONE");
        dto.setFirstName("A");
        dto.setLastName("B");
        dto.setPassword("p");

        assertThatThrownBy(() -> authenticationService.signup(dto))
                .hasMessageContaining("Phone number is required");
    }

    @Test
    void signup_email_requiresEmail() {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setVerificationMethod("EMAIL");
        dto.setFirstName("A");
        dto.setLastName("B");
        dto.setPassword("p");

        assertThatThrownBy(() -> authenticationService.signup(dto))
                .hasMessageContaining("Email is required");
    }

    @Test
    void authenticate_email_usesAuthenticationManager() {
        LoginUserDto dto = new LoginUserDto();
        dto.setEmail("a@b.com");
        dto.setPassword("secret");

        Customer user = new Customer();
        user.setEmail("a@b.com");
        user.setPassword("hash");
        user.setEnabled(true);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        User out = authenticationService.authenticate(dto);

        assertThat(out).isSameAs(user);
        verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken("a@b.com", "secret"));
    }

    @Test
    void authenticate_phone_matchesPasswordLocally() {
        LoginUserDto dto = new LoginUserDto();
        dto.setPhoneNumber("+1555");
        dto.setPassword("secret");

        Customer user = new Customer();
        user.setPhoneNumber("+1555");
        user.setPassword("hash");
        user.setEnabled(true);
        when(userRepository.findByPhoneNumber("+1555")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);

        User out = authenticationService.authenticate(dto);

        assertThat(out).isSameAs(user);
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void verifyUser_success_enablesUser() {
        VerifyUserDto dto = new VerifyUserDto();
        dto.setEmail("a@b.com");
        dto.setVerificationCode("111111");

        Customer user = new Customer();
        user.setVerificationCode("111111");
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        authenticationService.verifyUser(dto);

        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getVerificationCode()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void verifyUser_wrongCode_throws() {
        VerifyUserDto dto = new VerifyUserDto();
        dto.setEmail("a@b.com");
        dto.setVerificationCode("wrong");

        Customer user = new Customer();
        user.setVerificationCode("111111");
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authenticationService.verifyUser(dto))
                .hasMessageContaining("Invalid verification code");
    }

    @Test
    void createAdministrator_savesWhenEmailFree() {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setFirstName("A");
        dto.setLastName("B");
        dto.setEmail("admin@x.com");
        dto.setPassword("p");

        when(userRepository.findByEmail("admin@x.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("p")).thenReturn("h");
        Administrator admin = new Administrator();
        admin.setId(9L);
        when(userRepository.save(any(Administrator.class))).thenReturn(admin);

        User saved = authenticationService.createAdministrator(dto);

        assertThat(saved.getId()).isEqualTo(9L);
        ArgumentCaptor<Administrator> cap = ArgumentCaptor.forClass(Administrator.class);
        verify(userRepository).save(cap.capture());
        assertThat(cap.getValue().getEmail()).isEqualTo("admin@x.com");
    }

    @Test
    void emailExists_delegatesToRepository() {
        when(userRepository.findByEmail("x@y.com")).thenReturn(Optional.of(new Customer()));

        assertThat(authenticationService.emailExists("x@y.com")).isTrue();
    }

    @Test
    void signup_phone_success_sendsSms() throws Exception {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setFirstName("A");
        dto.setLastName("B");
        dto.setEmail("ignored@x.com");
        dto.setPassword("raw");
        dto.setPhoneNumber("+15550001");
        dto.setVerificationMethod("PHONE");

        when(userRepository.findByPhoneNumber("+15550001")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("raw")).thenReturn("hashed");
        Customer saved = new Customer();
        saved.setId(2L);
        saved.setPhoneNumber("+15550001");
        saved.setVerificationCode("111222");
        when(userRepository.save(any(Customer.class))).thenReturn(saved);

        authenticationService.signup(dto);

        verify(smsService).sendVerificationSms(eq("+15550001"), any());
        verify(emailService, never()).sendVerificationEmail(any(), any(), any());
    }

    @Test
    void signup_throwsWhenEmailAlreadyUsed() {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setFirstName("A");
        dto.setLastName("B");
        dto.setEmail("taken@x.com");
        dto.setPassword("p");
        dto.setVerificationMethod("EMAIL");
        when(userRepository.findByEmail("taken@x.com")).thenReturn(Optional.of(new Customer()));

        assertThatThrownBy(() -> authenticationService.signup(dto)).hasMessageContaining("Email already in use");
    }

    @Test
    void signup_throwsWhenPhoneAlreadyUsed() {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setFirstName("A");
        dto.setLastName("B");
        dto.setPassword("p");
        dto.setPhoneNumber("+1999");
        dto.setVerificationMethod("PHONE");
        when(userRepository.findByPhoneNumber("+1999")).thenReturn(Optional.of(new Customer()));

        assertThatThrownBy(() -> authenticationService.signup(dto)).hasMessageContaining("Phone number already in use");
    }

    @Test
    void authenticate_email_throwsWhenNotVerified() {
        LoginUserDto dto = new LoginUserDto();
        dto.setEmail("a@b.com");
        dto.setPassword("p");
        Customer user = new Customer();
        user.setEmail("a@b.com");
        user.setEnabled(false);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authenticationService.authenticate(dto)).hasMessageContaining("User not verified");
    }

    @Test
    void authenticate_email_throwsWhenUserMissing() {
        LoginUserDto dto = new LoginUserDto();
        dto.setEmail("missing@b.com");
        dto.setPassword("p");
        when(userRepository.findByEmail("missing@b.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.authenticate(dto)).hasMessageContaining("User not found");
    }

    @Test
    void authenticate_phone_throwsWhenBadPassword() {
        LoginUserDto dto = new LoginUserDto();
        dto.setPhoneNumber("+1555");
        dto.setPassword("bad");
        Customer user = new Customer();
        user.setPhoneNumber("+1555");
        user.setPassword("hash");
        user.setEnabled(true);
        when(userRepository.findByPhoneNumber("+1555")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.authenticate(dto)).hasMessageContaining("Invalid credentials");
    }

    @Test
    void verifyUser_phonePath_success() {
        VerifyUserDto dto = new VerifyUserDto();
        dto.setPhoneNumber("+1555");
        dto.setVerificationCode("999999");
        Customer user = new Customer();
        user.setVerificationCode("999999");
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(userRepository.findByPhoneNumber("+1555")).thenReturn(Optional.of(user));

        authenticationService.verifyUser(dto);

        assertThat(user.isEnabled()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void verifyUser_throwsWhenExpired() {
        VerifyUserDto dto = new VerifyUserDto();
        dto.setEmail("a@b.com");
        dto.setVerificationCode("111111");
        Customer user = new Customer();
        user.setVerificationCode("111111");
        user.setVerificationCodeExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authenticationService.verifyUser(dto)).hasMessageContaining("expired");
    }

    @Test
    void verifyUser_throwsWhenUserMissing() {
        VerifyUserDto dto = new VerifyUserDto();
        dto.setEmail("nope@b.com");
        dto.setVerificationCode("111111");
        when(userRepository.findByEmail("nope@b.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.verifyUser(dto)).hasMessageContaining("User not found");
    }

    @Test
    void resendVerificationCode_email_sendsEmail() throws Exception {
        Customer user = new Customer();
        user.setEnabled(false);
        user.setEmail("a@b.com");
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        authenticationService.resendVerificationCode("a@b.com", null);

        verify(emailService).sendVerificationEmail(eq("a@b.com"), any(), any());
    }

    @Test
    void resendVerificationCode_phone_sendsSms() {
        Customer user = new Customer();
        user.setEnabled(false);
        user.setPhoneNumber("+1888");
        when(userRepository.findByPhoneNumber("+1888")).thenReturn(Optional.of(user));
        when(userRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        authenticationService.resendVerificationCode(null, "+1888");

        verify(smsService).sendVerificationSms(eq("+1888"), any());
    }

    @Test
    void resendVerificationCode_throwsWhenAlreadyVerified() {
        Customer user = new Customer();
        user.setEnabled(true);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authenticationService.resendVerificationCode("a@b.com", null))
                .hasMessageContaining("already verified");
    }

    @Test
    void resendVerificationCode_throwsWhenUserMissing() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.resendVerificationCode("a@b.com", null))
                .hasMessageContaining("User not found");
    }

    @Test
    void sendVerificationEmail_wrapsEmailFailure() throws Exception {
        Customer user = new Customer();
        user.setFirstName("Pat");
        user.setEmail("a@b.com");
        user.setVerificationCode("123456");
        doThrow(new RuntimeException("smtp down")).when(emailService)
                .sendVerificationEmail(any(), any(), any());

        assertThatThrownBy(() -> authenticationService.sendVerificationEmail(user))
                .hasMessageContaining("Failed to send verification email");
    }

    @Test
    void sendVerificationSms_wrapsSmsFailure() {
        Customer user = new Customer();
        user.setPhoneNumber("+100");
        user.setVerificationCode("123456");
        doThrow(new RuntimeException("twilio down")).when(smsService)
                .sendVerificationSms(any(), any());

        assertThatThrownBy(() -> authenticationService.sendVerificationSms(user))
                .hasMessageContaining("Failed to send verification SMS");
    }

    @Test
    void createAdministrator_throwsWhenEmailTaken() {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setEmail("taken@x.com");
        when(userRepository.findByEmail("taken@x.com")).thenReturn(Optional.of(new Customer()));

        assertThatThrownBy(() -> authenticationService.createAdministrator(dto)).hasMessageContaining("Email already in use");
    }
}
