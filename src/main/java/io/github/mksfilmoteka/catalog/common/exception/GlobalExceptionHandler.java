package io.github.mksfilmoteka.catalog.common.exception;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.warn("Resource not found. path={}, message={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request, ErrorCode.NOT_FOUND));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            ConflictException ex, HttpServletRequest request) {

        log.warn("Conflict. path={}, message={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request, ErrorCode.CONFLICT));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(
            OptimisticLockingFailureException ex, HttpServletRequest request) {

        String message = "The resource was changed or deleted by another request";
        log.warn("Concurrent modification. path={}, message={}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildResponse(HttpStatus.CONFLICT, message, request, ErrorCode.CONFLICT));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        String message = "The request conflicts with existing data";
        log.warn("Data integrity violation. path={}, message={}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildResponse(HttpStatus.CONFLICT, message, request, ErrorCode.CONFLICT));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest webRequest) {

        HttpServletRequest request = servletRequest(webRequest);
        List<ErrorDetail> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> new ErrorDetail(
                        e.getField(),
                        e.getDefaultMessage()))
                .toList();

        ErrorResponse errorResponse =
                buildResponse("Validation failed", request, ErrorCode.VALIDATION_FAILED, details);

        log.warn("Validation failed. path={}", request.getRequestURI());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String message = String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName());
        List<ErrorDetail> errorDetails =
                List.of(new ErrorDetail(ex.getName(), expectedValueMessage(ex.getRequiredType())));

        log.warn("Invalid argument type. path={}", request.getRequestURI());
        return ResponseEntity.badRequest()
                .body(buildResponse(message, request, ErrorCode.BAD_REQUEST, errorDetails));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            @NonNull HttpMessageNotReadableException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest webRequest) {

        HttpServletRequest request = servletRequest(webRequest);
        String message = "Malformed request body";
        Throwable cause = ex.getMostSpecificCause();
        List<ErrorDetail> errorDetails = new ArrayList<>();

        if (cause instanceof InvalidFormatException invalidFormatException) {
            String field = extractFieldName(invalidFormatException);
            message = "Invalid value '%s' for field '%s'".formatted(invalidFormatException.getValue(), field);
            errorDetails.add(new ErrorDetail(field, expectedValueMessage(invalidFormatException.getTargetType())));
        }

        log.warn("Invalid value. path={}", request.getRequestURI());
        return ResponseEntity.badRequest()
                .body(buildResponse(message, request, ErrorCode.BAD_REQUEST, errorDetails));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex, HttpServletRequest request) {

        log.warn("Bad request. path={}, message={}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.badRequest()
                .body(buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, ErrorCode.BAD_REQUEST));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {

        String message = "Unexpected error occurred";

        log.error("Unexpected error. path={}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, message, request, ErrorCode.INTERNAL_ERROR));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            @NonNull Exception ex,
            @Nullable Object body,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode statusCode,
            @NonNull WebRequest webRequest) {

        HttpServletRequest request = servletRequest(webRequest);
        HttpStatus status = HttpStatus.valueOf(statusCode.value());

        if (status.is5xxServerError()) {
            log.error("Unexpected error. path={}", request.getRequestURI(), ex);
        } else {
            log.warn("Request rejected. path={}, status={}, message={}",
                    request.getRequestURI(), status.value(), ex.getMessage());
        }

        ErrorResponse errorResponse = buildResponse(status, resolveMessage(ex, status), request, resolveErrorCode(status));
        return super.handleExceptionInternal(ex, errorResponse, headers, statusCode, webRequest);
    }

    private String resolveMessage(Exception ex, HttpStatus status) {
        if (status.is5xxServerError()) {
            return "Unexpected error occurred";
        }
        if (ex instanceof org.springframework.web.ErrorResponse errorResponse
                && errorResponse.getBody().getDetail() != null) {
            return errorResponse.getBody().getDetail();
        }
        return status.getReasonPhrase();
    }

    private ErrorCode resolveErrorCode(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> ErrorCode.NOT_FOUND;
            case CONFLICT -> ErrorCode.CONFLICT;
            case METHOD_NOT_ALLOWED -> ErrorCode.METHOD_NOT_ALLOWED;
            case UNSUPPORTED_MEDIA_TYPE -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
            default -> status.is5xxServerError() ? ErrorCode.INTERNAL_ERROR : ErrorCode.BAD_REQUEST;
        };
    }

    private HttpServletRequest servletRequest(WebRequest webRequest) {
        return ((ServletWebRequest) webRequest).getRequest();
    }

    private ErrorResponse buildResponse(
            String message,
            HttpServletRequest request,
            ErrorCode code,
            List<ErrorDetail> errorDetails) {

        return new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                message,
                request.getRequestURI(),
                code,
                errorDetails);
    }

    private ErrorResponse buildResponse(
            HttpStatus status, String message, HttpServletRequest request, ErrorCode code) {

        return new ErrorResponse(
                LocalDateTime.now(), status.value(), message, request.getRequestURI(), code, List.of());
    }

    private String extractFieldName(InvalidFormatException ex) {
        return ex.getPath()
                .stream()
                .map(JacksonException.Reference::getPropertyName)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("requestBody");
    }

    private String expectedValueMessage(Class<?> targetType) {
        if (targetType != null && targetType.isEnum()) {
            return "Allowed values are: " + allowedValues(targetType);
        }

        return "Expected type: " + (targetType == null ? "valid value" : targetType.getSimpleName());
    }

    private String allowedValues(Class<?> enumType) {
        return Arrays.stream(enumType.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", "));
    }
}
