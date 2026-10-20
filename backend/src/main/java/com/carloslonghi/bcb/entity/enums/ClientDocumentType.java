package com.carloslonghi.bcb.entity.enums;

/**
 * Tipo do documento do cliente; precisa bater com o tamanho do documento (ClientService valida).
 */
public enum ClientDocumentType {
    // 11 digitos
    CPF,
    // 14 digitos
    CNPJ
}
