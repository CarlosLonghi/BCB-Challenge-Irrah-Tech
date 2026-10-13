package com.carloslonghi.bcb.utils;

import com.carloslonghi.bcb.entity.enums.ClientDocumentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"12345678909", "52998224725", "11144477735", "98765432100"})
    @DisplayName("aceita CPF com dígitos verificadores corretos")
    void validCpf(String cpf) {
        assertThat(DocumentValidator.isValidCpf(cpf)).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"12345678901", "52998224724", "11111111111", "00000000000", "1234567890",
            "123456789090", "1234567890a", "123.456.789-09", ""})
    @DisplayName("recusa CPF com dígito errado, repetido, tamanho errado, máscara ou nulo")
    void invalidCpf(String cpf) {
        assertThat(DocumentValidator.isValidCpf(cpf)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"11222333000181", "11444777000161", "04252011000110"})
    @DisplayName("aceita CNPJ com dígitos verificadores corretos")
    void validCnpj(String cnpj) {
        assertThat(DocumentValidator.isValidCnpj(cnpj)).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"11222333000182", "11222333000191", "12345678000199", "22222222222222",
            "1122233300018", "1122233300018a", "11.222.333/0001-81", ""})
    @DisplayName("recusa CNPJ com dígito errado, repetido, tamanho errado, máscara ou nulo")
    void invalidCnpj(String cnpj) {
        assertThat(DocumentValidator.isValidCnpj(cnpj)).isFalse();
    }

    @Test
    @DisplayName("isValid escolhe a regra pelo tipo do documento")
    void validByType() {
        assertThat(DocumentValidator.isValid(ClientDocumentType.CPF, "12345678909")).isTrue();
        assertThat(DocumentValidator.isValid(ClientDocumentType.CNPJ, "11222333000181")).isTrue();
        assertThat(DocumentValidator.isValid(ClientDocumentType.CPF, "11222333000181")).isFalse();
        assertThat(DocumentValidator.isValid(ClientDocumentType.CNPJ, "12345678909")).isFalse();
    }
}
