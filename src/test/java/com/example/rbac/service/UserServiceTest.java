package com.example.rbac.service;

import com.example.rbac.entity.User;
import com.example.rbac.exception.DuplicateResourceException;
import com.example.rbac.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
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

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("newuser", "encodedPassword");
        sampleUser.setId(1L);
    }

    @Test
    void createUser_succeeds_whenUsernameIsNew() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        User result = userService.createUser("newuser", "rawPassword");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("newuser", result.getUsername());
        verify(passwordEncoder).encode("rawPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_throwsConflict_whenUsernameAlreadyExists() {
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.createUser("newuser", "rawPassword"));

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }
}
