package br.com.fiap.phase4.commons.web.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponseDto(
        String errorCode,
        String message,
        List<String> details,
        String traceId,
        Instant timestamp
) {
    public static ErrorResponseDto of(String errorCode, String message, String traceId) {
        return new ErrorResponseDto(errorCode, message, List.of(), traceId, Instant.now());
    }

    public static ErrorResponseDto of(String errorCode, String message, List<String> details, String traceId) {
        return new ErrorResponseDto(errorCode, message, details, traceId, Instant.now());
    }
}
