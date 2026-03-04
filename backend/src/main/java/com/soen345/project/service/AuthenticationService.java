package com.soen345.project.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import com.soen345.project.dto.RegisterUserDto;
import com.soen345.project.dto.LoginUserDto;
import com.soen345.project.dto.VerifyUserDto;
import com.soen345.project.model.Customer;
import com.soen345.project.model.Administrator;
import com.soen345.project.model.User;
import com.soen345.project.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final SmsService smsService;


    public AuthenticationService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                 AuthenticationManager authenticationManager,
                                 EmailService emailService, SmsService smsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    public User signup(RegisterUserDto input) throws Exception {
        boolean usePhone = "PHONE".equalsIgnoreCase(input.getVerificationMethod());

        if (usePhone) {
            if (input.getPhoneNumber() == null || input.getPhoneNumber().isBlank()) {
                throw new RuntimeException("Phone number is required for phone verification");
            }
            if (userRepository.findByPhoneNumber(input.getPhoneNumber()).isPresent()) {
                throw new RuntimeException("Phone number already in use");
            }
        } else {
            if (input.getEmail() == null || input.getEmail().isBlank()) {
                throw new RuntimeException("Email is required for email verification");
            }
            if (userRepository.findByEmail(input.getEmail()).isPresent()) {
                throw new RuntimeException("Email already in use");
            }
        }

        Customer customer = new Customer();
        customer.setFirstName(input.getFirstName());
        customer.setLastName(input.getLastName());
        customer.setEmail(input.getEmail());
        customer.setPassword(passwordEncoder.encode(input.getPassword()));
        customer.setPhoneNumber(input.getPhoneNumber());
        customer.setEnabled(false);
        customer.setVerificationCode(generateVerificationCode());
        customer.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));

        User saved = userRepository.save(customer);

        if (usePhone) {
            sendVerificationSms(saved);
        } else {
            sendVerificationEmail(saved);
        }
        return saved;
    }

    public User authenticate(LoginUserDto input) {
        boolean usePhone = (input.getEmail() == null || input.getEmail().isBlank())
                && input.getPhoneNumber() != null && !input.getPhoneNumber().isBlank();

        User user;

        if (usePhone) {
            user = userRepository.findByPhoneNumber(input.getPhoneNumber())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            if (!user.isEnabled()) {
                throw new RuntimeException("User not verified");
            }
            if (!passwordEncoder.matches(input.getPassword(), user.getPassword())) {
                throw new RuntimeException("Invalid credentials");
            }
        } else {
            user = userRepository.findByEmail(input.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            if (!user.isEnabled()) {
                throw new RuntimeException("User not verified");
            }
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(input.getEmail(), input.getPassword())
            );
        }

        return user;
    }

    public void verifyUser(VerifyUserDto input) {
        boolean usePhone = (input.getEmail() == null || input.getEmail().isBlank())
                && input.getPhoneNumber() != null && !input.getPhoneNumber().isBlank();

        Optional<User> optionalUser = usePhone
                ? userRepository.findByPhoneNumber(input.getPhoneNumber())
                : userRepository.findByEmail(input.getEmail());

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Verification code expired");
            }
            if (user.getVerificationCode().equals(input.getVerificationCode())) {
                user.setEnabled(true);
                user.setVerificationCode(null);
                user.setVerificationCodeExpiresAt(null);
                userRepository.save(user);
            } else {
                throw new RuntimeException("Invalid verification code");
            }
        } else {
            throw new RuntimeException("User not found");
        }
    }

    public User createAdministrator(RegisterUserDto input) {
        if (userRepository.findByEmail(input.getEmail()).isPresent()) {
            throw new RuntimeException("Email already in use");
        }

        Administrator admin = new Administrator();
        admin.setFirstName(input.getFirstName());
        admin.setLastName(input.getLastName());
        admin.setEmail(input.getEmail());
        admin.setPassword(passwordEncoder.encode(input.getPassword()));
        admin.setEnabled(false);
        admin.setVerificationCode(generateVerificationCode());
        admin.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));

        return userRepository.save(admin);
    }

    public void resendVerificationCode(String email, String phoneNumber) {
        boolean usePhone = (email == null || email.isBlank())
                && phoneNumber != null && !phoneNumber.isBlank();

        Optional<User> optionalUser = usePhone
                ? userRepository.findByPhoneNumber(phoneNumber)
                : userRepository.findByEmail(email);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.isEnabled()) {
                throw new RuntimeException("User already verified");
            }
            user.setVerificationCode(generateVerificationCode());
            user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
            userRepository.save(user);
            if (usePhone) {
                sendVerificationSms(user);
            } else {
                sendVerificationEmail(user);
            }
        } else {
            throw new RuntimeException("User not found");
        }
    }

    public void sendVerificationEmail(User user) {
        String subject = "Eventigo Account Verification";
        String text = "Dear " + user.getFirstName() + ",\n\n"
                + "Thank you for registering with Eventigo! Please verify your account.\n\n"
                + "Verification Code: " + user.getVerificationCode() + "\n\n"
                + "This code will expire in 15 minutes.\n\n"
                + "Best regards,\n"
                + "The Eventigo Team";
        try {
            emailService.sendVerificationEmail(user.getEmail(), subject, text);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    public void sendVerificationSms(User user) {
        try {
            smsService.sendVerificationSms(((Customer) user).getPhoneNumber(), user.getVerificationCode());
        } catch (Exception e) {
            throw new RuntimeException("Failed to send verification SMS", e);
        }
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}