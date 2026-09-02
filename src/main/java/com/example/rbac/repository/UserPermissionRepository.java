package com.example.rbac.repository;

import com.example.rbac.entity.Permission;
import com.example.rbac.entity.User;
import com.example.rbac.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {
    List<UserPermission> findByUser(User user);
    boolean existsByUserAndPermission(User user, Permission permission);
}
