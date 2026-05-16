package com.dcim.platform.module.system.repository;

import com.dcim.platform.module.system.entity.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Long> {
    Optional<UserAccountEntity> findByUsername(String username);
    Optional<UserAccountEntity> findByEmail(String email);
}
