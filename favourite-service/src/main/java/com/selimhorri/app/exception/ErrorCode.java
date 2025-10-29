package com.selimhorri.app.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INTERNAL_SERVER_ERROR("ERR_1000", "An unexpected error occurred"),
    VALIDATION_ERROR("ERR_2000", "Validation failed for input data"),
    INVALID_INPUT("ERR_2001", "Invalid input provided"),
    DUPLICATE_RESOURCE("ERR_4003", "Resource already exists"),
    FAVOURITE_NOT_FOUND("ERR_3005", "Favourite with id %s not found");

    private final String code;
    private final String message;

    public String formatMessage(Object... args) {
        return String.format(message, args);
    }
}
