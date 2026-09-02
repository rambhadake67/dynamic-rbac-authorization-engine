package com.example.rbac.security;

import com.example.rbac.entity.User;
import com.example.rbac.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Loads a User from the database for Spring Security authentication.
 *
 * IMPORTANT: this class intentionally does NOT load roles/permissions as
 * GrantedAuthority objects. Authentication only proves "who you are".
 * Authorization ("what you can do") is decided separately, at the moment of
 * the request, by CustomPermissionEvaluator - not baked into the
 * Authentication object here. That is what makes the RBAC dynamic: if
 * permissions change in the database, the very next request is affected,
 * without the user needing to log in again.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // No authorities granted here on purpose - see class javadoc.
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(Collections.emptyList())
                .build();
    }
}
