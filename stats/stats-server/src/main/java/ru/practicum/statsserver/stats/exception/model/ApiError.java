package ru.practicum.statsserver.stats.exception.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@RequiredArgsConstructor
public class ApiError {
    private final HttpStatus status;
    private final String reason;
    private final String message;
}
