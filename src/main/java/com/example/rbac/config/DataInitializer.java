package com.example.rbac.config;

import com.example.rbac.entity.*;
import com.example.rbac.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner seedDatabase(UserRepository userRepository,
                                           RoleRepository roleRepository,
                                           PermissionRepository permissionRepository,
                                           RolePermissionRepository rolePermissionRepository,
                                           UserRoleRepository userRoleRepository,
                                           PasswordEncoder passwordEncoder) {
        return args -> {
            // ---- Permissions ----
            Permission createRole = getOrCreatePermission(permissionRepository, "CREATE_ROLE");
            Permission createPermission = getOrCreatePermission(permissionRepository, "CREATE_PERMISSION");
            Permission createUser = getOrCreatePermission(permissionRepository, "CREATE_USER");
            Permission assignRole = getOrCreatePermission(permissionRepository, "ASSIGN_ROLE");
            Permission assignPermission = getOrCreatePermission(permissionRepository, "ASSIGN_PERMISSION");
            Permission readSecureData = getOrCreatePermission(permissionRepository, "READ_SECURE_DATA");

            // ---- Roles ----
            Role adminRole = getOrCreateRole(roleRepository, "ADMIN");
            Role userRole = getOrCreateRole(roleRepository, "USER");

            // ---- Role -> Permission mappings ----
            grantIfMissing(rolePermissionRepository, adminRole, createRole);
            grantIfMissing(rolePermissionRepository, adminRole, createPermission);
            grantIfMissing(rolePermissionRepository, adminRole, createUser);
            grantIfMissing(rolePermissionRepository, adminRole, assignRole);
            grantIfMissing(rolePermissionRepository, adminRole, assignPermission);
            grantIfMissing(rolePermissionRepository, adminRole, readSecureData);

            grantIfMissing(rolePermissionRepository, userRole, readSecureData);

            // ---- Users ----
            User amit = userRepository.findByUsername("amit").orElseGet(() ->
                    userRepository.save(new User("amit", passwordEncoder.encode("amit123"))));

            User ram = userRepository.findByUsername("ram").orElseGet(() ->
                    userRepository.save(new User("ram", passwordEncoder.encode("ram123"))));

            // amit is bootstrapped as ADMIN so there is always at least one
            // account able to call the management APIs.
            if (!userRoleRepository.existsByUserAndRole(amit, adminRole)) {
                userRoleRepository.save(new UserRole(amit, adminRole));
            }

            // Ram is intentionally left with NO role at startup. Assigning
        };
    }

    private Permission getOrCreatePermission(PermissionRepository repo, String name) {
        return repo.findByName(name).orElseGet(() -> repo.save(new Permission(name)));
    }

    private Role getOrCreateRole(RoleRepository repo, String name) {
        return repo.findByName(name).orElseGet(() -> repo.save(new Role(name)));
    }

    private void grantIfMissing(RolePermissionRepository repo, Role role, Permission permission) {
        if (!repo.existsByRoleAndPermission(role, permission)) {
            repo.save(new RolePermission(role, permission));
        }
    }
}
