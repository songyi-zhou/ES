package org.zhou.backend.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.zhou.backend.repository.ClassRepository;
import org.zhou.backend.service.ClassService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClassServiceImpl implements ClassService {
    
    private final ClassRepository classRepository;
    
    @Override
    public List<String> getDistinctMajors() {
        return classRepository.findAll().stream()
            .map(schoolClass -> schoolClass.getMajor())
            .filter(major -> major != null && !major.isEmpty())
            .distinct()
            .collect(Collectors.toList());
    }
} 