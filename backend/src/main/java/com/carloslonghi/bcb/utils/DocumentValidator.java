package com.carloslonghi.bcb.utils;

import com.carloslonghi.bcb.entity.enums.ClientDocumentType;

// Confere os digitos verificadores de CPF e CNPJ (so numeros, sem mascara)
public final class DocumentValidator {

    private static final int[] CNPJ_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private DocumentValidator() {
    }

    public static boolean isValid(ClientDocumentType type, String document) {
        return type == ClientDocumentType.CPF ? isValidCpf(document) : isValidCnpj(document);
    }

    public static boolean isValidCpf(String cpf) {
        if (!hasValidFormat(cpf, 11)) {
            return false;
        }
        return cpfDigit(cpf, 9) == digitAt(cpf, 9) && cpfDigit(cpf, 10) == digitAt(cpf, 10);
    }

    public static boolean isValidCnpj(String cnpj) {
        if (!hasValidFormat(cnpj, 14)) {
            return false;
        }
        return cnpjDigit(cnpj, 12) == digitAt(cnpj, 12) && cnpjDigit(cnpj, 13) == digitAt(cnpj, 13);
    }

    // Sequencias de um digito so (ex.: 11111111111) passam no calculo, mas nao sao documentos validos
    private static boolean hasValidFormat(String document, int length) {
        return document != null
                && document.length() == length
                && document.chars().allMatch(Character::isDigit)
                && document.chars().distinct().count() > 1;
    }

    // Pesos decrescentes de (length + 1) ate 2 sobre os primeiros "length" digitos
    private static int cpfDigit(String cpf, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += digitAt(cpf, i) * (length + 1 - i);
        }
        int remainder = sum * 10 % 11;
        return remainder == 10 ? 0 : remainder;
    }

    // Primeiro digito usa os pesos 5,4,3,2,9..2; o segundo, 6,5,4,3,2,9..2
    private static int cnpjDigit(String cnpj, int length) {
        int offset = CNPJ_WEIGHTS.length - length;
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += digitAt(cnpj, i) * CNPJ_WEIGHTS[offset + i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static int digitAt(String document, int index) {
        return document.charAt(index) - '0';
    }
}
