package com.example.rbac.service;

import com.example.rbac.entity.Role;
import com.example.rbac.entity.User;
import com.example.rbac.exception.DuplicateResourceException;
import com.example.rbac.exception.ResourceNotFoundException;
import com.example.rbac.repository.RoleRepository;
import com.example.rbac.repository.UserRepository;
import com.example.rbac.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private UserRoleService userRoleService;

    private User ram;
    private Role userRole;

    @BeforeEach
    void setUp() {
        ram = new User("ram", "encoded");
        ram.setId(1L);
        userRole = new Role("USER");
        userRole.setId(1L);
    }

    @Test
    void assignRoleToUser_throwsNotFound_whenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userRoleService.assignRoleToUser(99L, 1L));
    }

    @Test
    void assignRoleToUser_throwsNotFound_whenRoleMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(ram));
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userRoleService.assignRoleToUser(1L, 99L));
    }

    @Test
    void assignRoleToUser_throwsConflict_whenAlreadyAssigned() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(ram));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(userRole));
        when(userRoleRepository.existsByUserAndRole(ram, userRole)).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> userRoleService.assignRoleToUser(1L, 1L));
    }

    @Test
    void assignRoleToUser_succeeds_whenValidAndNotDuplicate() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(ram));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(userRole));
        when(userRoleRepository.existsByUserAndRole(ram, userRole)).thenReturn(false);

        userRoleService.assignRoleToUser(1L, 1L);

        verify(userRoleRepository).save(any());
    }
}
