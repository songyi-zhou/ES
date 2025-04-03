package org.zhou.backend.service;

import org.springframework.stereotype.Service;
import org.zhou.backend.entity.EvaluationConfigLog;

@Service
public interface EvaluationConfigLogService {
    void saveLog(EvaluationConfigLog log);
} 