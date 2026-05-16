package com.dcim.platform.module.system.service;

import com.dcim.platform.module.system.entity.RoleEntity;
import com.dcim.platform.module.system.repository.RoleRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RoleService {

    private final RoleRepository repository;

    public RoleService(RoleRepository repository) {
        this.repository = repository;
    }

    public List<RoleEntity> list() {
        return repository.findAll();
    }

    public RoleEntity getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Role not found: " + id));
    }

    public RoleEntity create(RoleEntity entity) {
        return repository.save(entity);
    }

    public RoleEntity update(Long id, RoleEntity entity) {
        RoleEntity existing = getById(id);
        existing.setRoleCode(entity.getRoleCode());
        existing.setRoleName(entity.getRoleName());
        existing.setDescription(entity.getDescription());
        return repository.save(existing);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
