package com.orpe.consultants.dto;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomApiResponse {
    private String message;
    private boolean success;
    private LocalDateTime timestamp;
    private Object data;
    
    // Constructor for simple message + success
    public CustomApiResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
        this.timestamp = LocalDateTime.now();
    }
    
    // Constructor with data
    public CustomApiResponse(String message, boolean success, Object data) {
        this.message = message;
        this.success = success;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }
}
