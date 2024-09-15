package ru.practicum.mainservice.exception.handler;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.mainservice.exception.exception.*;
import ru.practicum.mainservice.exception.model.ApiError;

import java.util.List;
import java.util.Objects;

import static ru.practicum.mainservice.util.LogColorizeUtil.*;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFoundException(final NotFoundException e) {
        getLog(e,"NotFoundException");
        return new ApiError(HttpStatus.NOT_FOUND,
                "The required object was not found.",
                e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequestException(final BadRequestException e) {
        getLog(e,"BadRequestException");
        return new ApiError(HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleNotValidException(final MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> "Field: " + error.getField() + ". Error: " + error.getDefaultMessage() + ". Value: " + error.getRejectedValue())
                .toList();
        log.error("{}: {}", colorizeError("MethodArgumentNotValidException"), errors);
        return new ApiError(HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                String.join("; ", errors));
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleTypeMismatchException(final MethodArgumentTypeMismatchException e) {
        String message = "Failed to convert value of type " + Objects.requireNonNull(e.getValue()).getClass().getSimpleName() +
                " to required type " + Objects.requireNonNull(e.getRequiredType()).getSimpleName() +
                "; nested exception is " + e.getCause().getMessage();
        log.error("{}: {}", colorizeError("MethodArgumentTypeMismatchException"), message);
        return new ApiError(HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                message);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConstraintViolationException(final ConstraintViolationException e) {
        String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
        log.error("ConstraintViolationException: {}", message);
        return new ApiError(
                HttpStatus.CONFLICT,
                "Integrity constraint has been violated.",
                message
        );
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflictException(final ConflictException e) {
        getLog(e,"ConflictException");
        return new ApiError(HttpStatus.CONFLICT,
                "There are events associated with the category.",
                e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleViolationOfEditingRulesException(final ViolationOfEditingRulesException e) {
        getLog(e,"ViolationOfEditingRulesException");
        return new ApiError(HttpStatus.CONFLICT,
                "For the requested operation the conditions are not met.",
                e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleEventParticipationConstraintException(final EventParticipationConstraintException e) {
        getLog(e,"ConflictException");
        return new ApiError(HttpStatus.CONFLICT,
                "Restriction of participation in the event.",
                e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConstraintUpdatingException(final ConstraintUpdatingException e) {
        getLog(e,"ConflictException");
        return new ApiError(HttpStatus.CONFLICT,
                "Restriction of editing in the event.",
                e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDataIntegrityViolationException(final DataIntegrityViolationException e) {
        String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
        log.error("DataIntegrityViolationException: {}", message);
        return new ApiError(
                HttpStatus.CONFLICT,
                "Integrity constraint has been violated.",
                message
        );
    }

    private void getLog(RuntimeException e, String exceptionName) {
        StackTraceElement[] stackTrace = e.getStackTrace();
        if (stackTrace.length > 0) {
            StackTraceElement element = stackTrace[0];
            String methodName = element.getMethodName();
            String className = element.getClassName();
            int lastDotIndex = className.lastIndexOf('.');
            if (lastDotIndex != -1) {
                className = className.substring(lastDotIndex + 1);
            }

            log.error("{} in class {}, method {}: {}", colorizeError(exceptionName),colorizeClass(className), colorizeMethod(methodName), e.getMessage());
        } else {
            log.error("{}: {}",colorizeError(exceptionName), e.getMessage());
        }
    }
}
