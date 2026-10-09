package com.carloslonghi.bcb.mapper;

import com.carloslonghi.bcb.controller.request.ClientRequest;
import com.carloslonghi.bcb.controller.response.ClientBalanceResponse;
import com.carloslonghi.bcb.controller.response.ClientResponse;
import com.carloslonghi.bcb.entity.Client;
import com.carloslonghi.bcb.entity.enums.ClientDocumentType;
import com.carloslonghi.bcb.entity.enums.ClientPlanType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static com.carloslonghi.bcb.support.TestData.postPaidClient;
import static org.assertj.core.api.Assertions.assertThat;

class ClientMapperTest {

    private final ClientMapper mapper = Mappers.getMapper(ClientMapper.class);

    @Test
    @DisplayName("toEntity copia os dados do cadastro, sem id")
    void toEntity() {
        ClientRequest request = new ClientRequest("Ana", "12345678000199", ClientDocumentType.CNPJ,
                ClientPlanType.POST_PAID, BigDecimal.ZERO, new BigDecimal("50.00"), true);

        Client client = mapper.toEntity(request);

        assertThat(client.getId()).isNull();
        assertThat(client.getName()).isEqualTo("Ana");
        assertThat(client.getDocument()).isEqualTo("12345678000199");
        assertThat(client.getDocumentType()).isEqualTo(ClientDocumentType.CNPJ);
        assertThat(client.getPlanType()).isEqualTo(ClientPlanType.POST_PAID);
        assertThat(client.getLimit()).isEqualByComparingTo("50.00");
        assertThat(client.isActive()).isTrue();
    }

    @Test
    @DisplayName("toResponse e toBalanceResponse expõem saldo e limite")
    void toResponses() {
        Client client = postPaidClient(1L, "20.00");

        ClientResponse response = mapper.toResponse(client);
        ClientBalanceResponse balance = mapper.toBalanceResponse(client);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.planType()).isEqualTo(ClientPlanType.POST_PAID);
        assertThat(response.limit()).isEqualByComparingTo("20.00");
        assertThat(balance.balance()).isEqualByComparingTo("0");
        assertThat(balance.limit()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("entrada nula vira saída nula")
    void nullInput() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toResponse(null)).isNull();
        assertThat(mapper.toBalanceResponse(null)).isNull();
    }
}
