package com.dcim.platform.module.system.service;

import com.dcim.platform.module.system.entity.UserAccountEntity;
import com.dcim.platform.module.system.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserAccountService {

    private final UserAccountRepository repository;

    public UserAccountService(UserAccountRepository repository) {
        this.repository = repository;
    }

    public List<UserAccountEntity> list() {
        return repository.findAll();
    }

    public UserAccountEntity getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    public UserAccountEntity create(UserAccountEntity entity) {
        // TODO: 生产环境需要加密密码
        return repository.save(entity);
    }

    public UserAccountEntity update(Long id, UserAccountEntity entity) {
        UserAccountEntity existing = getById(id);
        existing.setUsername(entity.getUsername());
        // 不更新 passwordHash，后续单独提供改密接口
        existing.setDisplayName(entity.getDisplayName());
        existing.setEmail(entity.getEmail());
        existing.setPhone(entity.getPhone());
        existing.setStatus(entity.getStatus());
        existing.setDescription(entity.getDescription());
        return repository.save(existing);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
