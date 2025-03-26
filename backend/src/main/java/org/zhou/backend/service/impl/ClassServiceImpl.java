package org.zhou.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zhou.backend.repository.ClassRepository;
import org.zhou.backend.service.ClassService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassServiceImpl implements ClassService {
    
    private final ClassRepository classRepository;
    
    @Override
    public List<String> getDistinctMajors() {
        return classRepository.findDistinctMajors();
    }
} 