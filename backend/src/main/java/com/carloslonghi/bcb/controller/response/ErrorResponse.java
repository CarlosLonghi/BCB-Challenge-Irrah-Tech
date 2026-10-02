package com.carloslonghi.bcb.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Schema(description = "Corpo de erro devolvido pela API")
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,

        @Schema(description = "Texto para mostrar ao usuário", example = "Saldo insuficiente.")
        String message,

        String path
) {

    public static ErrorResponse of(HttpStatus status, String message, HttpServletRequest request) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI());
    }
}
