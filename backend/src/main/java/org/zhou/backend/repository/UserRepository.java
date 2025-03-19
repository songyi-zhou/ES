package org.zhou.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zhou.backend.entity.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserId(String userId);
} 