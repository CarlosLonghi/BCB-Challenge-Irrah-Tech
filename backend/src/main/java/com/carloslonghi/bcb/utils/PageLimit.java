package com.carloslonghi.bcb.utils;

import org.springframework.data.domain.Limit;

/**
 * Tamanho de pagina das listas: padrao 50, ajustado para 1..100 em vez de recusar o pedido com 400.
 */
public final class PageLimit {

    public static final int DEFAULT = 50;
    public static final int MAX = 100;

    private PageLimit() {
    }

    public static int clamp(int requested) {
        return Math.clamp(requested, 1, MAX);
    }

    public static Limit of(int requested) {
        return Limit.of(clamp(requested));
    }
}
