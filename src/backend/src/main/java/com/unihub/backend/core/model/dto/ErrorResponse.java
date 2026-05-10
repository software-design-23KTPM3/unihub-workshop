package com.unihub.backend.core.model.dto;

import java.time.ZonedDateTime;

public class ErrorResponse {
    private String message;
    private String code;
    private int status;
    private ZonedDateTime timestamp;
    private String path;

    public ErrorResponse() {}
    public ErrorResponse(String message, String code, int status, ZonedDateTime timestamp, String path) {
        this.message = message;
        this.code = code;
        this.status = status;
        this.timestamp = timestamp;
        this.path = path;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public ZonedDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(ZonedDateTime timestamp) { this.timestamp = timestamp; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public static ErrorResponseBuilder builder() { return new ErrorResponseBuilder(); }
    public static class ErrorResponseBuilder {
        private String message;
        private String code;
        private int status;
        private ZonedDateTime timestamp;
        private String path;
        public ErrorResponseBuilder message(String message) { this.message = message; return this; }
        public ErrorResponseBuilder code(String code) { this.code = code; return this; }
        public ErrorResponseBuilder status(int status) { this.status = status; return this; }
        public ErrorResponseBuilder timestamp(ZonedDateTime timestamp) { this.timestamp = timestamp; return this; }
        public ErrorResponseBuilder path(String path) { this.path = path; return this; }
        public ErrorResponse build() { return new ErrorResponse(message, code, status, timestamp, path); }
    }
}
