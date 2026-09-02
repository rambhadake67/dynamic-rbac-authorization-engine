package com.example.rbac.service;

import com.example.rbac.entity.Permission;
import com.example.rbac.entity.User;
import com.example.rbac.exception.DuplicateResourceException;
import com.example.rbac.exception.ResourceNotFoundException;
import com.example.rbac.repository.PermissionRepository;
import com.example.rbac.repository.UserPermissionRepository;
import com.example.rbac.repository.UserRepository;
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
class UserPermissionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private UserPermissionRepository userPermissionRepository;

    @InjectMocks
    private UserPermissionService userPermissionService;

    private User sampleUser;
    private Permission samplePermission;

    @BeforeEach
    void setUp() {
        sampleUser = new User("ram", "encodedPassword");
        sampleUser.setId(1L);

        samplePermission = new Permission("READ_SECURE_DATA");
        samplePermission.setId(2L);
    }

    @Test
    void assignPermissionToUser_succeeds_whenValidAndNotDuplicate() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(permissionRepository.findById(2L)).thenReturn(Optional.of(samplePermission));
        when(userPermissionRepository.existsByUserAndPermission(sampleUser, samplePermission)).thenReturn(false);

        userPermissionService.assignPermissionToUser(1L, 2L);

        verify(userPermissionRepository).save(any());
    }

    @Test
    void assignPermissionToUser_throwsNotFound_whenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userPermissionService.assignPermissionToUser(99L, 2L));
        verify(userPermissionRepository, never()).save(any());
    }

    @Test
    void assignPermissionToUser_throwsNotFound_whenPermissionMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(permissionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userPermissionService.assignPermissionToUser(1L, 99L));
        verify(userPermissionRepository, never()).save(any());
    }

    @Test
    void assignPermissionToUser_throwsConflict_whenAlreadyAssigned() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(permissionRepository.findById(2L)).thenReturn(Optional.of(samplePermission));
        when(userPermissionRepository.existsByUserAndPermission(sampleUser, samplePermission)).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> userPermissionService.assignPermissionToUser(1L, 2L));
        verify(userPermissionRepository, never()).save(any());
    }
}
