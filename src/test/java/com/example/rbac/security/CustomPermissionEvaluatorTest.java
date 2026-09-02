package com.example.rbac.security;

import com.example.rbac.entity.*;
import com.example.rbac.repository.RolePermissionRepository;
import com.example.rbac.repository.UserRepository;
import com.example.rbac.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomPermissionEvaluatorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CustomPermissionEvaluator evaluator;

    private User ram;
    private Role userRole;
    private Permission readSecureData;

    @BeforeEach
    void setUp() {
        ram = new User("ram", "encoded");
        ram.setId(1L);
        userRole = new Role("USER");
        userRole.setId(1L);
        readSecureData = new Permission("READ_SECURE_DATA");
        readSecureData.setId(1L);
    }

    @Test
    void grantsAccess_whenUserHasThePermissionThroughItsRole() {
        when(authentication.getName()).thenReturn("ram");
        when(userRepository.findByUsername("ram")).thenReturn(Optional.of(ram));
        when(userRoleRepository.findByUser(ram)).thenReturn(List.of(new UserRole(ram, userRole)));
        when(rolePermissionRepository.findByRole(userRole))
                .thenReturn(List.of(new RolePermission(userRole, readSecureData)));

        boolean result = evaluator.hasPermission(authentication, null, "READ_SECURE_DATA");

        assertTrue(result);
    }

    @Test
    void deniesAccess_whenPermissionWasRemovedFromRole() {
        when(authentication.getName()).thenReturn("ram");
        when(userRepository.findByUsername("ram")).thenReturn(Optional.of(ram));
        when(userRoleRepository.findByUser(ram)).thenReturn(List.of(new UserRole(ram, userRole)));
        // Role now has NO permissions - simulates permission removal from the DB.
        when(rolePermissionRepository.findByRole(userRole)).thenReturn(Collections.emptyList());

        boolean result = evaluator.hasPermission(authentication, null, "READ_SECURE_DATA");

        assertFalse(result);
    }

    @Test
    void deniesAccess_whenUserHasNoRoles() {
        when(authentication.getName()).thenReturn("ram");
        when(userRepository.findByUsername("ram")).thenReturn(Optional.of(ram));
        when(userRoleRepository.findByUser(ram)).thenReturn(Collections.emptyList());

        boolean result = evaluator.hasPermission(authentication, null, "READ_SECURE_DATA");

        assertFalse(result);
    }

    @Test
    void deniesAccess_whenUserDoesNotExist() {
        when(authentication.getName()).thenReturn("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        boolean result = evaluator.hasPermission(authentication, null, "READ_SECURE_DATA");

        assertFalse(result);
    }

    @Test
    void deniesAccess_whenAuthenticationIsNull() {
        assertFalse(evaluator.hasPermission(null, null, "READ_SECURE_DATA"));
    }
}
