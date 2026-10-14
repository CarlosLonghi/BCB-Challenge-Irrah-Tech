package com.carloslonghi.bcb.service;

import org.springframework.data.domain.Limit;
import com.carloslonghi.bcb.entity.Client;
import com.carloslonghi.bcb.entity.LedgerEntry;
import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.entity.enums.LedgerEntryType;
import com.carloslonghi.bcb.entity.enums.MessagePriority;
import com.carloslonghi.bcb.entity.enums.MessageStatus;
import com.carloslonghi.bcb.exception.ClientAccessDeniedException;
import com.carloslonghi.bcb.repository.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static com.carloslonghi.bcb.support.TestData.conversation;
import static com.carloslonghi.bcb.support.TestData.message;
import static com.carloslonghi.bcb.support.TestData.prePaidClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;
    @Mock
    private ClientService clientService;

    private LedgerService ledgerService;

    @BeforeEach
    void setUp() {
        ledgerService = new LedgerService(ledgerEntryRepository, clientService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("record grava o lançamento com cliente, mensagem, tipo, valor e hora")
    void recordsEntry() {
        Client client = prePaidClient(1L, "10.00");
        Message message = message(5L, conversation(7L, client), MessagePriority.NORMAL, MessageStatus.QUEUED);
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LedgerEntry entry = ledgerService.record(client, message, LedgerEntryType.DEBIT, new BigDecimal("0.25"));

        assertThat(entry.getClient()).isSameAs(client);
        assertThat(entry.getMessage()).isSameAs(message);
        assertThat(entry.getType()).isEqualTo(LedgerEntryType.DEBIT);
        assertThat(entry.getAmount()).isEqualByComparingTo("0.25");
        assertThat(entry.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("findByClient confere o acesso e devolve o extrato do cliente")
    void findsOwnLedger() {
        Client client = prePaidClient(1L, "10.00");
        List<LedgerEntry> entries = List.of(new LedgerEntry());
        when(clientService.findById(1L)).thenReturn(client);
        when(ledgerEntryRepository.findByClientIdOrderByIdDesc(1L, Limit.of(50))).thenReturn(entries);

        assertThat(ledgerService.findByClient(1L, null, 50)).isSameAs(entries);
    }

    @Test
    @DisplayName("findByClient com cursor busca os lançamentos anteriores a ele")
    void findsLedgerBeforeCursor() {
        List<LedgerEntry> entries = List.of(new LedgerEntry());
        when(clientService.findById(1L)).thenReturn(prePaidClient(1L, "10.00"));
        when(ledgerEntryRepository.findByClientIdAndIdLessThanOrderByIdDesc(1L, 30L, Limit.of(10))).thenReturn(entries);

        assertThat(ledgerService.findByClient(1L, 30L, 10)).isSameAs(entries);
    }

    @Test
    @DisplayName("extrato de outro cliente: o acesso negado propaga sem consultar lançamentos")
    void otherClientLedger() {
        when(clientService.findById(2L)).thenThrow(new ClientAccessDeniedException());

        assertThatThrownBy(() -> ledgerService.findByClient(2L, null, 50))
                .isInstanceOf(ClientAccessDeniedException.class);
        verifyNoInteractions(ledgerEntryRepository);
    }
}
