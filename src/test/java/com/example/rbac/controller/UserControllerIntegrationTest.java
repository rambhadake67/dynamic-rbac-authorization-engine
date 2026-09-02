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
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createUser_succeeds_forAdminWithCreateUserPermission() throws Exception {
        mockMvc.perform(post("/users")
                        .with(httpBasic("amit", "amit123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newuser1\",\"password\":\"pass123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser1"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void createUser_isForbidden_forUserWithoutCreateUserPermission() throws Exception {
        mockMvc.perform(post("/users")
                        .with(httpBasic("ram", "ram123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"forbiddenUser\",\"password\":\"pass123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUser_isUnauthorized_withoutCredentials() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"unauthuser\",\"password\":\"pass123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createUser_isConflict_whenUsernameAlreadyExists() throws Exception {
        mockMvc.perform(post("/users")
                        .with(httpBasic("amit", "amit123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dupuser\",\"password\":\"pass123\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/users")
                        .with(httpBasic("amit", "amit123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dupuser\",\"password\":\"pass123\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void createUser_isBadRequest_whenFieldsAreBlank() throws Exception {
        mockMvc.perform(post("/users")
                        .with(httpBasic("amit", "amit123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
