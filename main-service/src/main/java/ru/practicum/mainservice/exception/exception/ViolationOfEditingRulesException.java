package ru.practicum.mainservice.exception.exception;

public class ViolationOfEditingRulesException extends RuntimeException {
    public ViolationOfEditingRulesException(String message) {
        super(message);
    }
}
