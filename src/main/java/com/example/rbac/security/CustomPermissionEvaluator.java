package com.example.rbac.security;

import com.example.rbac.entity.Role;
import com.example.rbac.entity.RolePermission;
import com.example.rbac.entity.User;
import com.example.rbac.entity.UserPermission;
import com.example.rbac.entity.UserRole;
import com.example.rbac.repository.RolePermissionRepository;
import com.example.rbac.repository.UserPermissionRepository;
import com.example.rbac.repository.UserRepository;
import com.example.rbac.repository.UserRoleRepository;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * This is the heart of the "dynamic" part of this Dynamic RBAC engine.
 *
 * Every time a method annotated with
 *   @PreAuthorize("hasPermission(null, 'SOME_PERMISSION')")
 * is invoked, Spring Security calls hasPermission(...) below. There is no
 * hardcoded role/permission name anywhere in this class - everything is
 * looked up from the database at request time:
 *
 *   1) Direct permission: User -> UserPermission -> Permission
 *   2) Role-based permission: User -> UserRole -> Role -> RolePermission -> Permission
 *
 * Because the check is done fresh on every request, changing the database
 * instantly changes what a currently logged-in user is allowed to do, with zero code changes and no restart.
 */
@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserPermissionRepository userPermissionRepository;

    public CustomPermissionEvaluator(UserRepository userRepository,
                                      UserRoleRepository userRoleRepository,
                                      RolePermissionRepository rolePermissionRepository,
                                      UserPermissionRepository userPermissionRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userPermissionRepository = userPermissionRepository;
    }

    /**
     * Used by expressions like hasPermission(null, 'READ_SECURE_DATA').
     * targetDomainObject is ignored here since our permissions are not tied
     * to a specific entity instance, only to the current user.
     */
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || permission == null) {
            return false;
        }
        String requiredPermission = permission.toString();
        String username = authentication.getName();

        return userHasPermission(username, requiredPermission);
    }

    /**
     * Overload used by expressions like
     * hasPermission(#id, 'SomeType', 'SOME_PERMISSION'). Not used by this
     * project today, but implemented for completeness since it is part of
     * the PermissionEvaluator contract.
     */
    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (authentication == null || permission == null) {
            return false;
        }
        return userHasPermission(authentication.getName(), permission.toString());
    }

    private boolean userHasPermission(String username, String requiredPermission) {
        // 1. Find the authenticated user in the database.
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return false;
        }
        User user = userOpt.get();

        // 2. Check permissions directly assigned to the user (User -> UserPermission -> Permission)
        List<UserPermission> userPermissions = userPermissionRepository.findByUser(user);
        for (UserPermission userPermission : userPermissions) {
            if (userPermission.getPermission().getName().equals(requiredPermission)) {
                return true;
            }
        }

        // 3. Find every role assigned to this user (User -> UserRole -> Role).
        List<UserRole> userRoles = userRoleRepository.findByUser(user);

        // 4. For each role, find its permissions (Role -> RolePermission -> Permission)
        //    and check if the required permission is among them.
        for (UserRole userRole : userRoles) {
            Role role = userRole.getRole();
            List<RolePermission> rolePermissions = rolePermissionRepository.findByRole(role);
            for (RolePermission rolePermission : rolePermissions) {
                if (rolePermission.getPermission().getName().equals(requiredPermission)) {
                    // Permission found -> ALLOW.
                    return true;
                }
            }
        }

        // 5. Permission not found anywhere for this user -> DENY.
        return false;
    }
}
