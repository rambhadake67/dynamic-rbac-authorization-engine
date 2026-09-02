package com.example.rbac.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Shared request body shape for both "create role" and "create permission":
 * { "name": "SOME_NAME" }
 */
public class NameRequest {

    @NotBlank(message = "name must not be blank")
    private String name;

    public NameRequest() {
    }

    public NameRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
