package com.example.rbac.controller;

import com.example.rbac.dto.CreateUserRequest;
import com.example.rbac.dto.UserResponse;
import com.example.rbac.entity.User;
import com.example.rbac.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // POST /users -> requires CREATE_USER permission
    @PostMapping("/users")
    @PreAuthorize("hasPermission(null, 'CREATE_USER')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(request.getUsername(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserResponse(user.getId(), user.getUsername()));
    }
}
