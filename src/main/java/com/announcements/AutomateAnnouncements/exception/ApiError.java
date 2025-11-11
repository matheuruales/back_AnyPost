package com.announcements.AutomateAnnouncements.exception;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

@Data
public class ApiError {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private Map<String, String> errors;
    private String message;
    private String path;

    public ApiError() {
        this.timestamp = LocalDateTime.now();
    }
}