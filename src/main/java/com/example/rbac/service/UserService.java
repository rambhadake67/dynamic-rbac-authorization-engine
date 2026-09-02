package com.example.rbac.service;

import com.example.rbac.entity.User;
import com.example.rbac.exception.DuplicateResourceException;
import com.example.rbac.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(String username, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("User already exists: " + username);
        }
        String encodedPassword = passwordEncoder.encode(rawPassword);
        return userRepository.save(new User(username, encodedPassword));
    }
}
