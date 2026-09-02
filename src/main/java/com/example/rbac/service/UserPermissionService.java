package com.example.rbac.service;

import com.example.rbac.entity.Permission;
import com.example.rbac.entity.User;
import com.example.rbac.entity.UserPermission;
import com.example.rbac.exception.DuplicateResourceException;
import com.example.rbac.exception.ResourceNotFoundException;
import com.example.rbac.repository.PermissionRepository;
import com.example.rbac.repository.UserPermissionRepository;
import com.example.rbac.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserPermissionService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final UserPermissionRepository userPermissionRepository;

    public UserPermissionService(UserRepository userRepository,
                                 PermissionRepository permissionRepository,
                                 UserPermissionRepository userPermissionRepository) {
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.userPermissionRepository = userPermissionRepository;
    }

    public void assignPermissionToUser(Long userId, Long permissionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + permissionId));

        if (userPermissionRepository.existsByUserAndPermission(user, permission)) {
            throw new DuplicateResourceException(
                    "Permission '" + permission.getName() + "' is already assigned to user '" + user.getUsername() + "'");
        }

        userPermissionRepository.save(new UserPermission(user, permission));
    }
}
