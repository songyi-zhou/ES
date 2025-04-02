package org.zhou.backend.service;

import org.zhou.backend.entity.User;

public interface UserService {
    User getCurrentUser();
    User createUser(String userId, String password, String name);
    User getUserById(Long id);
    User findByUsername(String username);
} 