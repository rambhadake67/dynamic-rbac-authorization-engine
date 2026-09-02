package com.example.rbac.controller;

import com.example.rbac.entity.Permission;
import com.example.rbac.entity.User;
import com.example.rbac.repository.PermissionRepository;
import com.example.rbac.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserPermissionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PermissionRepository permissionRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void assignPermissionToUser_succeeds_andGrantsAccessDirectlyToUser() throws Exception {
        User permUser = userRepository.save(new User("permuser", passwordEncoder.encode("pw123")));
        Permission readDataPermission = permissionRepository.findByName("READ_SECURE_DATA").orElseThrow();

        // 1. Verify user cannot access secure data initially
        mockMvc.perform(get("/secure-data")
                        .with(httpBasic("permuser", "pw123")))
                .andExpect(status().isForbidden());

        // 2. Admin assigns permission directly to user
        mockMvc.perform(post("/users/" + permUser.getId() + "/permissions/" + readDataPermission.getId())
                        .with(httpBasic("amit", "amit123")))
                .andExpect(status().isOk());

        // 3. Verify user can now access secure data immediately via direct user permission
        mockMvc.perform(get("/secure-data")
                        .with(httpBasic("permuser", "pw123")))
                .andExpect(status().isOk());
    }

    @Test
    void assignPermissionToUser_returns404_whenUserDoesNotExist() throws Exception {
        Permission readDataPermission = permissionRepository.findByName("READ_SECURE_DATA").orElseThrow();

        mockMvc.perform(post("/users/999999/permissions/" + readDataPermission.getId())
                        .with(httpBasic("amit", "amit123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void assignPermissionToUser_isForbidden_withoutAssignPermissionPermission() throws Exception {
        Permission readDataPermission = permissionRepository.findByName("READ_SECURE_DATA").orElseThrow();
        User ram = userRepository.findByUsername("ram").orElseThrow();

        mockMvc.perform(post("/users/" + ram.getId() + "/permissions/" + readDataPermission.getId())
                        .with(httpBasic("ram", "ram123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignPermissionToUser_isConflict_whenAlreadyAssigned() throws Exception {
        User permUser2 = userRepository.save(new User("permuser2", passwordEncoder.encode("pw123")));
        Permission readDataPermission = permissionRepository.findByName("READ_SECURE_DATA").orElseThrow();

        mockMvc.perform(post("/users/" + permUser2.getId() + "/permissions/" + readDataPermission.getId())
                        .with(httpBasic("amit", "amit123")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/users/" + permUser2.getId() + "/permissions/" + readDataPermission.getId())
                        .with(httpBasic("amit", "amit123")))
                .andExpect(status().isConflict());
    }
}
