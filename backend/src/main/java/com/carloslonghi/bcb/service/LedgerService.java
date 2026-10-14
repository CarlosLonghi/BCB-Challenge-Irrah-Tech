package com.carloslonghi.bcb.service;

import com.carloslonghi.bcb.entity.Client;
import com.carloslonghi.bcb.entity.LedgerEntry;
import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.entity.enums.LedgerEntryType;
import com.carloslonghi.bcb.repository.LedgerEntryRepository;
import com.carloslonghi.bcb.utils.PageLimit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final ClientService clientService;
    private final Clock clock;

    // Chamado dentro da transacao que alterou o saldo/limite: lancamento e saldo andam juntos
    public LedgerEntry record(Client client, Message message, LedgerEntryType type, BigDecimal amount) {
        LedgerEntry entry = new LedgerEntry();
        entry.setClient(client);
        entry.setMessage(message);
        entry.setType(type);
        entry.setAmount(amount);
        entry.setCreatedAt(clock.instant());
        return ledgerEntryRepository.save(entry);
    }

    // Extrato do proprio cliente, mais recente primeiro; outro id vira 403, como no saldo
    public List<LedgerEntry> findByClient(Long clientId, Long before, int limit) {
        Client client = clientService.findById(clientId);
        return before == null
                ? ledgerEntryRepository.findByClientIdOrderByIdDesc(client.getId(), PageLimit.of(limit))
                : ledgerEntryRepository.findByClientIdAndIdLessThanOrderByIdDesc(client.getId(), before, PageLimit.of(limit));
    }
}
