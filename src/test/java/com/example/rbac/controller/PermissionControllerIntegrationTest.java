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
class PermissionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createPermission_succeeds_forAdmin() throws Exception {
        mockMvc.perform(post("/permissions")
                        .with(httpBasic("amit", "amit123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"DELETE_SECURE_DATA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("DELETE_SECURE_DATA"));
    }

    @Test
    void createPermission_isForbidden_withoutCreatePermissionPermission() throws Exception {
        mockMvc.perform(post("/permissions")
                        .with(httpBasic("ram", "ram123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"SOME_PERMISSION\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createPermission_isConflict_onDuplicateName() throws Exception {
        mockMvc.perform(post("/permissions")
                        .with(httpBasic("amit", "amit123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"DUPLICATE_PERM\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/permissions")
                        .with(httpBasic("amit", "amit123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"DUPLICATE_PERM\"}"))
                .andExpect(status().isConflict());
    }
}
