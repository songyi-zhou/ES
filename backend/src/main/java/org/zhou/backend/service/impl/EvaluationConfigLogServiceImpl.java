package org.zhou.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zhou.backend.entity.EvaluationConfigLog;
import org.zhou.backend.repository.EvaluationConfigLogRepository;
import org.zhou.backend.service.EvaluationConfigLogService;

@Service
@RequiredArgsConstructor
public class EvaluationConfigLogServiceImpl implements EvaluationConfigLogService {
    private final EvaluationConfigLogRepository logRepository;
    
    @Override
    public void saveLog(EvaluationConfigLog log) {
        logRepository.save(log);
    }
} 