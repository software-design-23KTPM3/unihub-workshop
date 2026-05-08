package com.unihub.backend.core.exception;

import com.unihub.backend.core.model.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.ZonedDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildErrorResponse(ex.getMessage(), "ACCESS_DENIED", HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(WorkshopAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleWorkshopAccessDenied(WorkshopAccessDeniedException ex, HttpServletRequest request) {
        return buildErrorResponse(ex.getMessage(), "WORKSHOP_ACCESS_DENIED", HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(WorkshopNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWorkshopNotFound(WorkshopNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(ex.getMessage(), "WORKSHOP_NOT_FOUND", HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(InvalidWorkshopException.class)
    public ResponseEntity<ErrorResponse> handleInvalidWorkshop(InvalidWorkshopException ex, HttpServletRequest request) {
        return buildErrorResponse(ex.getMessage(), "INVALID_WORKSHOP", HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(RegistrationConflictException.class)
    public ResponseEntity<ErrorResponse> handleRegistrationConflict(RegistrationConflictException ex, HttpServletRequest request) {
        return buildErrorResponse(ex.getMessage(), "REGISTRATION_CONFLICT", HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(WorkshopSoldOutException.class)
    public ResponseEntity<ErrorResponse> handleWorkshopSoldOut(WorkshopSoldOutException ex, HttpServletRequest request) {
        return buildErrorResponse(ex.getMessage(), "WORKSHOP_SOLD_OUT", HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePayment(PaymentException ex, HttpServletRequest request) {
        return buildErrorResponse(ex.getMessage(), "PAYMENT_ERROR", HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(PaymentGatewayUnavailableException.class)
    public ResponseEntity<ErrorResponse> handlePaymentGatewayUnavailable(PaymentGatewayUnavailableException ex, HttpServletRequest request) {
        return buildErrorResponse(ex.getMessage(), "PAYMENT_GATEWAY_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildErrorResponse(message, "VALIDATION_ERROR", HttpStatus.UNPROCESSABLE_ENTITY, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        return buildErrorResponse(ex.getMessage(), "INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(String message, String code, HttpStatus status, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .message(message)
                .code(code)
                .status(status.value())
                .timestamp(ZonedDateTime.now())
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(error, status);
    }
}
