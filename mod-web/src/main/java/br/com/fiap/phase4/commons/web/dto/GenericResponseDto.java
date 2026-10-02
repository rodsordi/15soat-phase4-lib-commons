package br.com.fiap.phase4.commons.web.dto;

import java.time.Instant;

public record GenericResponseDto<T>(
        T data,
        String message,
        Instant timestamp
) {
    public static <T> GenericResponseDto<T> success(T data) {
        return new GenericResponseDto<>(data, "Operation completed successfully", Instant.now());
    }

    public static <T> GenericResponseDto<T> success(T data, String message) {
        return new GenericResponseDto<>(data, message, Instant.now());
    }
}
