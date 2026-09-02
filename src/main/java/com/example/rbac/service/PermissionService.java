package com.example.rbac.service;

import com.example.rbac.entity.Permission;
import com.example.rbac.exception.DuplicateResourceException;
import com.example.rbac.repository.PermissionRepository;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;

    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    public Permission createPermission(String name) {
        if (permissionRepository.existsByName(name)) {
            throw new DuplicateResourceException("Permission already exists: " + name);
        }
        return permissionRepository.save(new Permission(name));
    }
}
