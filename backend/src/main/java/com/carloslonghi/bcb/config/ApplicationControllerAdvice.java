package com.carloslonghi.bcb.config;

import com.carloslonghi.bcb.controller.response.ErrorResponse;
import com.carloslonghi.bcb.exception.ClientAccessDeniedException;
import com.carloslonghi.bcb.exception.ClientNotActiveException;
import com.carloslonghi.bcb.exception.CreditLimitExceededException;
import com.carloslonghi.bcb.exception.DocumentAlreadyExistsException;
import com.carloslonghi.bcb.exception.DocumentNotRegisteredException;
import com.carloslonghi.bcb.exception.InsufficientBalanceException;
import com.carloslonghi.bcb.exception.InvalidDocumentException;
import com.carloslonghi.bcb.exception.MissingRecipientException;
import com.carloslonghi.bcb.exception.ReferencedEntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

// Erros que nao passam por um controller (ex.: 401 do TokenAuthenticationFilter) seguem pelo
// /error padrao do Spring Boot, que tem o mesmo formato do ErrorResponse.
@RestControllerAdvice
public class ApplicationControllerAdvice {

    @ExceptionHandler(DocumentNotRegisteredException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleDocumentNotRegisteredException(DocumentNotRegisteredException exception,
                                                              HttpServletRequest request) {
        return ErrorResponse.of(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
    }

    @ExceptionHandler(ClientNotActiveException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleClientNotActiveException(ClientNotActiveException exception,
                                                        HttpServletRequest request) {
        return ErrorResponse.of(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    @ExceptionHandler(ClientAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleClientAccessDeniedException(ClientAccessDeniedException exception,
                                                           HttpServletRequest request) {
        return ErrorResponse.of(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    @ExceptionHandler(ReferencedEntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleReferencedEntityNotFoundException(ReferencedEntityNotFoundException exception,
                                                                 HttpServletRequest request) {
        return ErrorResponse.of(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    @ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
    public ErrorResponse handleInsufficientBalanceException(InsufficientBalanceException exception,
                                                            HttpServletRequest request) {
        return ErrorResponse.of(HttpStatus.PAYMENT_REQUIRED, exception.getMessage(), request);
    }

    @ExceptionHandler(CreditLimitExceededException.class)
    @ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
    public ErrorResponse handleCreditLimitExceededException(CreditLimitExceededException exception,
                                                            HttpServletRequest request) {
        return ErrorResponse.of(HttpStatus.PAYMENT_REQUIRED, exception.getMessage(), request);
    }

    @ExceptionHandler(DocumentAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDocumentAlreadyExistsException(DocumentAlreadyExistsException exception,
                                                              HttpServletRequest request) {
        return ErrorResponse.of(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidDocumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidDocumentException(InvalidDocumentException exception,
                                                        HttpServletRequest request) {
        return ErrorResponse.of(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(MissingRecipientException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingRecipientException(MissingRecipientException exception,
                                                         HttpServletRequest request) {
        return ErrorResponse.of(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    // @Valid falhou: junta as mensagens de todos os campos invalidos
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException exception,
                                                               HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .sorted()
                .collect(Collectors.joining("; "));
        return ErrorResponse.of(HttpStatus.BAD_REQUEST, message, request);
    }

    // JSON malformado ou enum com valor desconhecido (ex.: "priority": "ALTA")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadableException(HttpMessageNotReadableException exception,
                                                               HttpServletRequest request) {
        return ErrorResponse.of(HttpStatus.BAD_REQUEST,
                "Corpo da requisição inválido. Confira o JSON e os valores dos campos.", request);
    }

    // Rede de seguranca para a constraint unique do banco (ex.: dois cadastros simultaneos do mesmo documento)
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDataIntegrityViolationException(DataIntegrityViolationException exception,
                                                               HttpServletRequest request) {
        return ErrorResponse.of(HttpStatus.CONFLICT,
                "Violação de integridade de dados. Verifique se os dados enviados não conflitam com um registro já existente.",
                request);
    }
}
