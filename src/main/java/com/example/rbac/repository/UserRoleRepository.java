package com.example.rbac.repository;

import com.example.rbac.entity.Role;
import com.example.rbac.entity.User;
import com.example.rbac.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findByUser(User user);
    boolean existsByUserAndRole(User user, Role role);
}
