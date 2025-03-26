package org.zhou.backend.model.response;

import java.util.List;

import lombok.Data;

@Data
public class ImportResult {
    private int successCount;
    private List<String> errors;
} 