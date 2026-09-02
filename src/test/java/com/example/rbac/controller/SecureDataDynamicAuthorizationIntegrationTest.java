package com.example.rbac.controller;

import com.example.rbac.entity.*;
import com.example.rbac.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * This is the single most important test in the project: it proves that
 * authorization is driven ENTIRELY by the database, with no restart and no
 * Java code changes required, exactly as demonstrated manually via Postman
 * in the README (sections "Dynamic Authorization Test" / "Permission
 * Removal").
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecureDataDynamicAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PermissionRepository permissionRepository;
    @Autowired
    private RolePermissionRepository rolePermissionRepository;
    @Autowired
    private UserRoleRepository userRoleRepository;

    @Test
    void secureData_isUnauthorized_withoutCredentials() throws Exception {
        mockMvc.perform(get("/secure-data"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void secureData_flipsBetween200And403_purelyByEditingDatabaseRows() throws Exception {
        // DataInitializer has already seeded: USER role -> READ_SECURE_DATA
        // permission, and "ram" user with no role assigned yet.
        User ram = userRepository.findByUsername("ram").orElseThrow();
        Role userRole = roleRepository.findByName("USER").orElseThrow();
        Permission readSecureData = permissionRepository.findByName("READ_SECURE_DATA").orElseThrow();

        // 1) Ram has no role yet -> 403 Forbidden
        mockMvc.perform(get("/secure-data").with(httpBasic("ram", "ram123")))
                .andExpect(status().isForbidden());

        // 2) Assign USER role to Ram directly through the database (simulates
        //    the POST /users/{userId}/roles/{roleId} API) -> now 200 OK
        userRoleRepository.save(new UserRole(ram, userRole));
        mockMvc.perform(get("/secure-data").with(httpBasic("ram", "ram123")))
                .andExpect(status().isOk());

        // 3) Remove READ_SECURE_DATA from the USER role directly in the DB
        //    (no Java code touched) -> back to 403 Forbidden
        RolePermission mapping = rolePermissionRepository.findByRole(userRole).stream()
                .filter(rp -> rp.getPermission().getName().equals("READ_SECURE_DATA"))
                .findFirst()
                .orElseThrow();
        rolePermissionRepository.delete(mapping);
        mockMvc.perform(get("/secure-data").with(httpBasic("ram", "ram123")))
                .andExpect(status().isForbidden());

        // 4) Re-add the permission -> 200 OK again, proving the decision is
        //    always re-evaluated live against the database.
        rolePermissionRepository.save(new RolePermission(userRole, readSecureData));
        mockMvc.perform(get("/secure-data").with(httpBasic("ram", "ram123")))
                .andExpect(status().isOk());
    }

    @Test
    void secureData_isOk_forAdminUser_whoHasThePermissionTransitively() throws Exception {
        // amit is seeded as ADMIN, and ADMIN has READ_SECURE_DATA too.
        mockMvc.perform(get("/secure-data").with(httpBasic("amit", "amit123")))
                .andExpect(status().isOk());
    }
}
