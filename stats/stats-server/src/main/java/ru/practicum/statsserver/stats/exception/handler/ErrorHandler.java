package ru.practicum.statsserver.stats.exception.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.practicum.statsserver.stats.exception.exception.BadRequestException;
import ru.practicum.statsserver.stats.exception.model.ApiError;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {
    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequestException(final BadRequestException e) {
        return new ApiError(HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                e.getMessage());
    }
}
