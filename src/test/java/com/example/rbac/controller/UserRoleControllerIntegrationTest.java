package com.example.rbac.controller;

import com.example.rbac.entity.Role;
import com.example.rbac.entity.User;
import com.example.rbac.repository.RoleRepository;
import com.example.rbac.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserRoleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void assignRoleToUser_succeeds_forAdmin() throws Exception {
        User extraUser = userRepository.save(new User("extrauser", passwordEncoder.encode("pw123")));
        Role userRole = roleRepository.findByName("USER").orElseThrow();

        mockMvc.perform(post("/users/" + extraUser.getId() + "/roles/" + userRole.getId())
                        .with(httpBasic("amit", "amit123")))
                .andExpect(status().isOk());
    }

    @Test
    void assignRoleToUser_returns404_whenUserDoesNotExist() throws Exception {
        Role userRole = roleRepository.findByName("USER").orElseThrow();

        mockMvc.perform(post("/users/999999/roles/" + userRole.getId())
                        .with(httpBasic("amit", "amit123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void assignRoleToUser_isForbidden_withoutAssignRolePermission() throws Exception {
        Role userRole = roleRepository.findByName("USER").orElseThrow();
        User ram = userRepository.findByUsername("ram").orElseThrow();

        mockMvc.perform(post("/users/" + ram.getId() + "/roles/" + userRole.getId())
                        .with(httpBasic("ram", "ram123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignRoleToUser_isConflict_whenAlreadyAssigned() throws Exception {
        User extraUser2 = userRepository.save(new User("extrauser2", passwordEncoder.encode("pw123")));
        Role userRole = roleRepository.findByName("USER").orElseThrow();

        mockMvc.perform(post("/users/" + extraUser2.getId() + "/roles/" + userRole.getId())
                        .with(httpBasic("amit", "amit123")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/users/" + extraUser2.getId() + "/roles/" + userRole.getId())
                        .with(httpBasic("amit", "amit123")))
                .andExpect(status().isConflict());
    }
}
