package com.carloslonghi.bcb.entity.enums;

/**
 * Plano do cliente, definido no cadastro. Decide de onde ClientService.debit/refund tiram e devolvem o custo.
 */
public enum ClientPlanType {
    // Gasta do saldo (balance); sem saldo, 402
    PRE_PAID,
    // Gasta do limite (limit), que guarda quanto ainda resta; sem limite, 402
    POST_PAID
}
