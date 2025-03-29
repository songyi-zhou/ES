package org.zhou.backend.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zhou.backend.model.dto.RoleDTO;
import org.zhou.backend.repository.RoleRepository;

@RestController
@RequestMapping("/api/roles")
public class RoleController {
    private final RoleRepository roleRepository;

    public RoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @GetMapping("/student")
    public List<RoleDTO> getStudentRoles() {
        return roleRepository.findByRoleLevelLessThan(3)
            .stream()
            .map(role -> {
                String value = role.getRoleName()
                    .replace("ROLE_", "")
                    .toLowerCase()
                    .replace("_", "");
                if (value.equals("student")) value = "user";
                return new RoleDTO(value, role.getDescription());
            })
            .collect(Collectors.toList());
    }
} 