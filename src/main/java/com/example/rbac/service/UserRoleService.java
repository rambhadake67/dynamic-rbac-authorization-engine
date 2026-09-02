package com.example.rbac.service;

import com.example.rbac.entity.Role;
import com.example.rbac.entity.User;
import com.example.rbac.entity.UserRole;
import com.example.rbac.exception.DuplicateResourceException;
import com.example.rbac.exception.ResourceNotFoundException;
import com.example.rbac.repository.RoleRepository;
import com.example.rbac.repository.UserRepository;
import com.example.rbac.repository.UserRoleRepository;
import org.springframework.stereotype.Service;

@Service
public class UserRoleService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    public UserRoleService(UserRepository userRepository,
                            RoleRepository roleRepository,
                            UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    public void assignRoleToUser(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        if (userRoleRepository.existsByUserAndRole(user, role)) {
            throw new DuplicateResourceException(
                    "Role '" + role.getName() + "' is already assigned to user '" + user.getUsername() + "'");
        }

        userRoleRepository.save(new UserRole(user, role));
    }
}
