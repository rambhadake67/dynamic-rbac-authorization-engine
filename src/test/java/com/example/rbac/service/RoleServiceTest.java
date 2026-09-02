package com.example.rbac.service;

import com.example.rbac.entity.Permission;
import com.example.rbac.entity.Role;
import com.example.rbac.exception.DuplicateResourceException;
import com.example.rbac.exception.ResourceNotFoundException;
import com.example.rbac.repository.PermissionRepository;
import com.example.rbac.repository.RolePermissionRepository;
import com.example.rbac.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @InjectMocks
    private RoleService roleService;

    private Role adminRole;
    private Permission createRolePermission;

    @BeforeEach
    void setUp() {
        adminRole = new Role("ADMIN");
        adminRole.setId(1L);
        createRolePermission = new Permission("CREATE_ROLE");
        createRolePermission.setId(1L);
    }

    @Test
    void createRole_succeeds_whenNameIsNew() {
        when(roleRepository.existsByName("ADMIN")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenReturn(adminRole);

        Role result = roleService.createRole("ADMIN");

        assertEquals("ADMIN", result.getName());
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void createRole_throwsConflict_whenNameAlreadyExists() {
        when(roleRepository.existsByName("ADMIN")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> roleService.createRole("ADMIN"));
        verify(roleRepository, never()).save(any());
    }

    @Test
    void assignPermissionToRole_throwsNotFound_whenRoleMissing() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> roleService.assignPermissionToRole(99L, 1L));
    }

    @Test
    void assignPermissionToRole_throwsNotFound_whenPermissionMissing() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));
        when(permissionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> roleService.assignPermissionToRole(1L, 99L));
    }

    @Test
    void assignPermissionToRole_throwsConflict_whenAlreadyAssigned() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(createRolePermission));
        when(rolePermissionRepository.existsByRoleAndPermission(adminRole, createRolePermission)).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> roleService.assignPermissionToRole(1L, 1L));
    }

    @Test
    void assignPermissionToRole_succeeds_whenValidAndNotDuplicate() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(createRolePermission));
        when(rolePermissionRepository.existsByRoleAndPermission(adminRole, createRolePermission)).thenReturn(false);

        roleService.assignPermissionToRole(1L, 1L);

        verify(rolePermissionRepository).save(any());
    }
}
