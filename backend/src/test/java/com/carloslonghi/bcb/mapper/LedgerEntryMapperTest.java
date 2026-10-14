package com.carloslonghi.bcb.mapper;

import com.carloslonghi.bcb.controller.response.LedgerEntryResponse;
import com.carloslonghi.bcb.entity.Client;
import com.carloslonghi.bcb.entity.LedgerEntry;
import com.carloslonghi.bcb.entity.enums.LedgerEntryType;
import com.carloslonghi.bcb.entity.enums.MessagePriority;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.Instant;

import static com.carloslonghi.bcb.support.TestData.conversation;
import static com.carloslonghi.bcb.support.TestData.message;
import static com.carloslonghi.bcb.support.TestData.prePaidClient;
import static org.assertj.core.api.Assertions.assertThat;

class LedgerEntryMapperTest {

    private final LedgerEntryMapper mapper = Mappers.getMapper(LedgerEntryMapper.class);

    @Test
    @DisplayName("toResponse expõe o id da mensagem, não a entidade")
    void toResponse() {
        Client client = prePaidClient(1L, "10.00");
        LedgerEntry entry = new LedgerEntry();
        entry.setId(3L);
        entry.setClient(client);
        entry.setMessage(message(9L, conversation(7L, client), MessagePriority.NORMAL, MessageStatus.FAILED));
        entry.setType(LedgerEntryType.REFUND);
        entry.setAmount(new BigDecimal("0.25"));
        entry.setCreatedAt(Instant.parse("2026-10-14T12:00:00Z"));

        LedgerEntryResponse response = mapper.toResponse(entry);

        assertThat(response).isEqualTo(new LedgerEntryResponse(3L, 9L, LedgerEntryType.REFUND,
                new BigDecimal("0.25"), Instant.parse("2026-10-14T12:00:00Z")));
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    @DisplayName("lançamento sem mensagem carregada vira messageId nulo")
    void withoutMessage() {
        assertThat(mapper.toResponse(new LedgerEntry()).messageId()).isNull();
    }
}
