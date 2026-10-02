package com.carloslonghi.bcb.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados que o cliente pode alterar. Saldo, limite, plano e documento são definidos no cadastro e não entram aqui (campos extras no JSON são ignorados)")
public record ClientUpdateRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String name
) {
}
