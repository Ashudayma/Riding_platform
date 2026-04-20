package com.ridingplatform.common.web;

import com.ridingplatform.driver.application.DriverStateException;
import com.ridingplatform.admin.application.AdminNotFoundException;
import com.ridingplatform.fraud.application.FraudNotFoundException;
import com.ridingplatform.fraud.application.FraudReviewException;
import com.ridingplatform.notification.application.NotificationNotFoundException;
import com.ridingplatform.ride.application.RideNotFoundException;
import com.ridingplatform.ride.application.RideStateTransitionException;
import com.ridingplatform.ride.application.RideValidationException;
import com.ridingplatform.security.application.IdempotencyConflictException;
import com.ridingplatform.security.application.MissingIdempotencyKeyException;
import com.ridingplatform.security.application.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        var response = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed",
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(RideValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleRideValidation(RideValidationException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        ));
    }

    @ExceptionHandler(DriverStateException.class)
    public ResponseEntity<ApiErrorResponse> handleDriverValidation(DriverStateException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        ));
    }

    @ExceptionHandler(RideNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleRideNotFound(RideNotFoundException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorResponse(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        ));
    }

    @ExceptionHandler(RideStateTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleRideStateTransition(RideStateTransitionException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorResponse(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        ));
    }

    @ExceptionHandler(FraudNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleFraudNotFound(FraudNotFoundException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorResponse(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        ));
    }

    @ExceptionHandler(FraudReviewException.class)
    public ResponseEntity<ApiErrorResponse> handleFraudReview(FraudReviewException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorResponse(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        ));
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotificationNotFound(NotificationNotFoundException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorResponse(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        ));
    }

    @ExceptionHandler(AdminNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleAdminNotFound(AdminNotFoundException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorResponse(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        ));
    }

    @ExceptionHandler({
            MissingRequestHeaderException.class,
            MissingIdempotencyKeyException.class,
            IdempotencyConflictException.class,
            RateLimitExceededException.class
    })
    public ResponseEntity<ApiErrorResponse> handleClientSecurityErrors(Exception exception, HttpServletRequest request) {
        HttpStatus status = exception instanceof RateLimitExceededException
                ? HttpStatus.TOO_MANY_REQUESTS
                : exception instanceof IdempotencyConflictException
                ? HttpStatus.CONFLICT
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiErrorResponse(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "Access denied",
                request.getRequestURI()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception at path={} message={}", request.getRequestURI(), exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiErrorResponse(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected error occurred",
                request.getRequestURI()
        ));
    }
}
