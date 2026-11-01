package com.hcl.stockanalysis.service;

import com.hcl.stockanalysis.dto.AuthResponse;
import com.hcl.stockanalysis.dto.LoginRequest;
import com.hcl.stockanalysis.dto.RegisterRequest;
import com.hcl.stockanalysis.entity.User;
import com.hcl.stockanalysis.exception.UserAlreadyExistsException;
import com.hcl.stockanalysis.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Registration encodes password and persists user with BCrypt hash instead of plain text")
    void register_success_persistsHashedPassword() {
        RegisterRequest request = new RegisterRequest("john_doe", "SecretPass123!", "john@example.com");

        when(userRepository.existsByUsername("john_doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecretPass123!")).thenReturn("$2a$10$hashedPasswordHereMockedString");

        AuthResponse response = userService.register(request);

        assertThat(response.getUsername()).isEqualTo("john_doe");
        assertThat(response.getRole()).isEqualTo("ROLE_USER");
        assertThat(response.getMessage()).isEqualTo("User registered successfully");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("john_doe");
        assertThat(savedUser.getEmail()).isEqualTo("john@example.com");
        // Must be hashed, NEVER plain text
        assertThat(savedUser.getPassword()).isEqualTo("$2a$10$hashedPasswordHereMockedString");
        assertThat(savedUser.getPassword()).isNotEqualTo("SecretPass123!");
    }

    @Test
    @DisplayName("Registering an existing username throws UserAlreadyExistsException")
    void register_duplicateUsername_throwsUserAlreadyExistsException() {
        RegisterRequest request = new RegisterRequest("alice", "pass123", "alice_new@example.com");

        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("Username already exists: alice");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registering an existing email throws UserAlreadyExistsException")
    void register_duplicateEmail_throwsUserAlreadyExistsException() {
        RegisterRequest request = new RegisterRequest("newuser", "pass123", "alice@example.com");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("Email already registered: alice@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login with valid credentials succeeds and returns AuthResponse")
    void login_validCredentials_succeeds() {
        User user = User.builder()
                .id(1L)
                .username("alice")
                .password("$2a$10$hashedPassword")
                .role("ROLE_USER")
                .build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("alice123", "$2a$10$hashedPassword")).thenReturn(true);

        AuthResponse response = userService.login(new LoginRequest("alice", "alice123"));

        assertThat(response.getUsername()).isEqualTo("alice");
        assertThat(response.getRole()).isEqualTo("ROLE_USER");
        assertThat(response.getMessage()).isEqualTo("Login successful");
    }

    @Test
    @DisplayName("Login with wrong password throws BadCredentialsException")
    void login_wrongPassword_throwsBadCredentialsException() {
        User user = User.builder()
                .id(1L)
                .username("alice")
                .password("$2a$10$hashedPassword")
                .role("ROLE_USER")
                .build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", "$2a$10$hashedPassword")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(new LoginRequest("alice", "wrongPass")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    @DisplayName("Login with unknown username throws BadCredentialsException")
    void login_unknownUser_throwsBadCredentialsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(new LoginRequest("unknown", "anyPass")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid username or password");
    }
}
