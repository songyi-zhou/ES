package org.zhou.backend.service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.zhou.backend.dto.LoginRequest;
import org.zhou.backend.dto.LoginResponse;
import org.zhou.backend.entity.User;
import org.zhou.backend.repository.UserRepository;
import org.zhou.backend.security.JwtTokenProvider;
import org.zhou.backend.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    // 角色常量
    private static final int ROLE_STUDENT = 0;
    private static final int ROLE_GROUP_MEMBER = 1;
    private static final int ROLE_GROUP_LEADER = 2;
    private static final int ROLE_COUNSELOR = 3;
    private static final int ROLE_ADMIN = 4;

    public LoginResponse authenticateAndGenerateToken(LoginRequest loginRequest) throws AuthenticationException {
//         添加日志来检查密码匹配
         log.debug("Attempting to authenticate user: {}", loginRequest.getUserId());
         log.debug("Raw password matches stored hash: {}",
             passwordEncoder.matches(loginRequest.getPassword(),
                 "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTTql21ZyG2"));

         // 先检查用户是否存在
         Optional<User> userOptional = userRepository.findByUserId(loginRequest.getUserId());
         if (userOptional.isEmpty() ||
             !passwordEncoder.matches(loginRequest.getPassword(), userOptional.get().getPassword())) {
             throw new BadCredentialsException("用户名或密码错误");
         }

        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getUserId(),
                loginRequest.getPassword()
            )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return new LoginResponse(
            jwt,
            userPrincipal.getUserId(),
            userPrincipal.getName(),
            userPrincipal.getRoleLevel()
        );
    }

    public User registerUser(LoginRequest request) {
        if (userRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new RuntimeException("用户ID已存在");
        }

        User user = new User();
        user.setUserId(request.getUserId());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        // 根据用户ID前缀设置不同的角色
        Set<String> roles = new HashSet<>();
        if (request.getUserId().startsWith("admin")) {
            user.setRoleLevel(ROLE_ADMIN);
            roles.add("ROLE_ADMIN");
            user.setName("管理员");
            user.setMajor(null);
            user.setClassName(null);
        } else if (request.getUserId().startsWith("T")) {
            user.setRoleLevel(ROLE_COUNSELOR);
            roles.add("ROLE_COUNSELOR");
            user.setName("导员");
            user.setClassName(null);
        } else {
            // 默认为学生
            user.setRoleLevel(ROLE_STUDENT);
            roles.add("ROLE_STUDENT");
            user.setName("学生");
        }

        // 设置通用默认值
        user.setDepartment("信息工程学院");
        if (user.getMajor() == null) {
            user.setMajor("计算机科学与技术");
        }
        if (user.getClassName() == null) {
            user.setClassName("计科2401");
        }
        
        user.setRoles(roles);

        log.info("Registering new user with ID: {} and role level: {}", 
                request.getUserId(), user.getRoleLevel());
        return userRepository.save(user);
    }
}