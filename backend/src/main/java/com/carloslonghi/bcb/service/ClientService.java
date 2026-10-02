package com.carloslonghi.bcb.service;

import com.carloslonghi.bcb.exception.ClientAccessDeniedException;
import com.carloslonghi.bcb.exception.CreditLimitExceededException;
import com.carloslonghi.bcb.exception.DocumentAlreadyExistsException;
import com.carloslonghi.bcb.exception.InsufficientBalanceException;
import com.carloslonghi.bcb.exception.InvalidDocumentException;
import com.carloslonghi.bcb.exception.ReferencedEntityNotFoundException;
import com.carloslonghi.bcb.entity.Client;
import com.carloslonghi.bcb.entity.enums.ClientDocumentType;
import com.carloslonghi.bcb.entity.enums.ClientPlanType;
import com.carloslonghi.bcb.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;

    public Client create(Client client) {
        validateDocumentType(client);
        if (clientRepository.existsByDocument(client.getDocument())) {
            throw new DocumentAlreadyExistsException(client.getDocument());
        }

        return clientRepository.save(client);
    }

    // Cada cliente so enxerga a si mesmo: a lista tem um item so
    public List<Client> findAuthenticated() {
        return clientRepository.findById(getAuthenticatedClientId())
                .stream()
                .toList();
    }

    public Client findById(Long id) {
        checkIsAuthenticatedClient(id);
        return clientRepository.findById(id)
                .orElseThrow(() -> new ReferencedEntityNotFoundException("Cliente", id));
    }

    // O cliente so altera o proprio nome: saldo, limite e plano mudariam o que ele pode gastar
    public Client updateName(Long id, String name) {
        Client client = findById(id);

        client.setName(name);

        return clientRepository.save(client);
    }

    public void debit(Client client, BigDecimal amount) {
        if (client.getPlanType() == ClientPlanType.PRE_PAID) {
            if (client.getBalance().compareTo(amount) < 0) {
                throw new InsufficientBalanceException();
            }
            client.setBalance(client.getBalance().subtract(amount));
        } else {
            BigDecimal remainingLimit = client.getLimit().subtract(amount);
            if (remainingLimit.compareTo(BigDecimal.ZERO) < 0) {
                throw new CreditLimitExceededException();
            }
            client.setLimit(remainingLimit);
        }

        clientRepository.save(client);
    }

    private Long getAuthenticatedClientId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private void checkIsAuthenticatedClient(Long id) {
        if (!id.equals(getAuthenticatedClientId())) {
            throw new ClientAccessDeniedException();
        }
    }

    private void validateDocumentType(Client client) {
        int expectedLength = client.getDocumentType() == ClientDocumentType.CPF ? 11 : 14;
        if (client.getDocument().length() != expectedLength) {
            throw new InvalidDocumentException(client.getDocumentType().name(), expectedLength);
        }
    }
}
