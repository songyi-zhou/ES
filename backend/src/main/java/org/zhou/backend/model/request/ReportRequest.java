package org.zhou.backend.model.request;

import java.util.List;

import lombok.Data;

@Data
public class ReportRequest {
    private List<Long> materialIds;
    private String note;
}

