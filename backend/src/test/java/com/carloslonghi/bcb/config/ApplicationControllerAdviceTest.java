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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ApplicationControllerAdviceTest {

    private final ApplicationControllerAdvice advice = new ApplicationControllerAdvice();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/messages");

    private static void assertError(ErrorResponse error, HttpStatus status, String message) {
        assertThat(error.status()).isEqualTo(status.value());
        assertThat(error.error()).isEqualTo(status.getReasonPhrase());
        assertThat(error.message()).isEqualTo(message);
        assertThat(error.path()).isEqualTo("/messages");
        assertThat(error.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("saldo insuficiente e limite excedido viram 402")
    void paymentRequired() {
        assertError(advice.handleInsufficientBalanceException(new InsufficientBalanceException(), request),
                HttpStatus.PAYMENT_REQUIRED, "Saldo insuficiente.");
        assertError(advice.handleCreditLimitExceededException(new CreditLimitExceededException(), request),
                HttpStatus.PAYMENT_REQUIRED, "Limite de consumo excedido.");
    }

    @Test
    @DisplayName("erros de autenticação e acesso viram 401 e 403")
    void authErrors() {
        assertError(advice.handleDocumentNotRegisteredException(new DocumentNotRegisteredException(), request),
                HttpStatus.UNAUTHORIZED, "Documento não cadastrado. Verifique o CPF/CNPJ.");
        assertError(advice.handleClientNotActiveException(new ClientNotActiveException(), request),
                HttpStatus.FORBIDDEN, "Cliente inativo.");
        assertError(advice.handleClientAccessDeniedException(new ClientAccessDeniedException(), request),
                HttpStatus.FORBIDDEN, "Acesso permitido somente aos dados do próprio cliente.");
    }

    @Test
    @DisplayName("not found, conflito e requisição inválida")
    void otherBusinessErrors() {
        assertError(advice.handleReferencedEntityNotFoundException(new ReferencedEntityNotFoundException("Conversa", 7L), request),
                HttpStatus.NOT_FOUND, "Conversa de id 7 não encontrado(a).");
        assertError(advice.handleDocumentAlreadyExistsException(new DocumentAlreadyExistsException("12345678901"), request),
                HttpStatus.CONFLICT, "Já existe cliente com o documento 12345678901.");
        assertError(advice.handleInvalidDocumentException(new InvalidDocumentException("CNPJ", 14), request),
                HttpStatus.BAD_REQUEST, "CNPJ deve ter 14 dígitos.");
        assertError(advice.handleMissingRecipientException(new MissingRecipientException(), request),
                HttpStatus.BAD_REQUEST, "Para iniciar uma conversa informe recipientId e recipientName.");
    }

    @Test
    @DisplayName("@Valid junta as mensagens dos campos inválidos em ordem alfabética")
    void validationErrors() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "priority", "Prioridade é obrigatória (NORMAL ou URGENT)"));
        bindingResult.addError(new FieldError("request", "content", "Mensagem não pode ser vazia"));
        MethodParameter parameter = new MethodParameter(Object.class.getMethod("equals", Object.class), 0);

        ErrorResponse error = advice.handleMethodArgumentNotValidException(
                new MethodArgumentNotValidException(parameter, bindingResult), request);

        assertError(error, HttpStatus.BAD_REQUEST,
                "Mensagem não pode ser vazia; Prioridade é obrigatória (NORMAL ou URGENT)");
    }

    @Test
    @DisplayName("JSON inválido e violação de integridade têm mensagens fixas")
    void fixedMessages() {
        assertError(advice.handleHttpMessageNotReadableException(
                        new HttpMessageNotReadableException("JSON quebrado", mock(HttpInputMessage.class)), request),
                HttpStatus.BAD_REQUEST, "Corpo da requisição inválido. Confira o JSON e os valores dos campos.");
        assertError(advice.handleDataIntegrityViolationException(new DataIntegrityViolationException("unique"), request),
                HttpStatus.CONFLICT,
                "Violação de integridade de dados. Verifique se os dados enviados não conflitam com um registro já existente.");
    }
}
