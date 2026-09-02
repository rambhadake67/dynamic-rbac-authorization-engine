package com.example.rbac.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createRole_succeeds_forAdminWithCreateRolePermission() throws Exception {
        mockMvc.perform(post("/roles")
                        .with(httpBasic("amit", "amit123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"MANAGER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("MANAGER"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void createRole_isForbidden_forUserWithoutCreateRolePermission() throws Exception {
        // Ram has no role/permission assigned by default in a fresh context.
        mockMvc.perform(post("/roles")
                        .with(httpBasic("ram", "ram123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"SOME_ROLE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createRole_isUnauthorized_withoutCredentials() throws Exception {
        mockMvc.perform(post("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"SOME_ROLE\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createRole_isConflict_whenRoleNameAlreadyExists() throws Exception {
        mockMvc.perform(post("/roles")
                        .with(httpBasic("amit", "amit123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"DUPLICATE_ROLE\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/roles")
                        .with(httpBasic("amit", "amit123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"DUPLICATE_ROLE\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void createRole_isBadRequest_whenNameIsBlank() throws Exception {
        mockMvc.perform(post("/roles")
                        .with(httpBasic("amit", "amit123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assignPermissionToRole_returns404_whenRoleDoesNotExist() throws Exception {
        mockMvc.perform(post("/roles/99999/permissions/1")
                        .with(httpBasic("amit", "amit123")))
                .andExpect(status().isNotFound());
    }
}
