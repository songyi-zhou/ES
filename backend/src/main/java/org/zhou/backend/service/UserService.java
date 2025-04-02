package org.zhou.backend.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.zhou.backend.entity.User;
import org.zhou.backend.exception.ResourceNotFoundException;
import org.zhou.backend.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class 
UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public User createUser(String userId, String password, String name) {
        if (userRepository.findByUserId(userId).isPresent()) {
            throw new RuntimeException("用户ID已存在");
        }

        User user = new User();
        user.setUserId(userId);
        user.setPassword(passwordEncoder.encode(password));
        user.setName(name);
        user.setDepartment("信息工程学院");
        
        // 根据用户ID设置角色和其他信息
        Set<String> roles = new HashSet<>();
        if (userId.startsWith("admin")) {
            user.setRoleLevel(4);
            roles.add("ROLE_ADMIN");
            user.setMajor(null);
            user.setClassName(null);
        } else if (userId.startsWith("T")) {
            user.setRoleLevel(3);
            roles.add("ROLE_COUNSELOR");
            user.setMajor("计算机科学与技术");
            user.setClassName(null);
        } else {
            user.setRoleLevel(0);
            roles.add("ROLE_STUDENT");
            user.setMajor("计算机科学与技术");
            user.setClassName("计科2401");
        }
        
        user.setRoles(roles);
        return userRepository.save(user);
    }
    
    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
    
    public User findByUsername(String username) {
        return userRepository.findByUserId(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }
    
    // 用户相关的业务逻辑方法将在这里实现
} 