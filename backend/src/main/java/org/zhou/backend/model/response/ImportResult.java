package org.zhou.backend.model.response;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ImportResult {
    private int successCount = 0;
    private int failureCount = 0;
    private List<String> errors = new ArrayList<>();

    public void incrementSuccessCount() {
        this.successCount++;
    }

    public void incrementFailureCount() {
        this.failureCount++;
    }
} 