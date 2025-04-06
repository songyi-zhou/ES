package org.zhou.backend.model.dto;

import lombok.Data;

@Data
public class ResponseDTO<T> {
    private boolean success;
    private T data;
    
    public static <T> ResponseDTO<T> success(T data) {
        ResponseDTO<T> response = new ResponseDTO<>();
        response.setSuccess(true);
        response.setData(data);
        return response;
    }
} 