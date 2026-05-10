package com.unihub.backend.core.exception;

public class WorkshopSoldOutException extends RuntimeException {
    public WorkshopSoldOutException(String message) {
        super(message);
    }
}
