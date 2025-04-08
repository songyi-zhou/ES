package org.zhou.backend.model.dto;

import lombok.Data;

@Data
public class ResponseDTO<T> {
    private boolean success;
    private String message;
    private T data;
    
    public static <T> ResponseDTO<T> success(T data) {
        ResponseDTO<T> response = new ResponseDTO<>();
        response.setSuccess(true);
        response.setData(data);
        return response;
    }
    
    public static <T> ResponseDTO<T> error(String message) {
        ResponseDTO<T> response = new ResponseDTO<>();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
} 