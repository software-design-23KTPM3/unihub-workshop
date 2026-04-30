package com.unihub.backend.core.exception;

public class WorkshopAccessDeniedException extends RuntimeException {
    public WorkshopAccessDeniedException() {
        super("You do not have permission to manage this workshop");
    }
}
