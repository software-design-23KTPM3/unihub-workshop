package com.unihub.backend.core.exception;

public class WorkshopNotFoundException extends RuntimeException {
    public WorkshopNotFoundException() {
        super("Workshop not found");
    }
}
