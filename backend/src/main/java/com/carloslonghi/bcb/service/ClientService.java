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
import com.carloslonghi.bcb.utils.DocumentValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cadastro e consulta de clientes e movimentacao de saldo/limite.
 * <p>
 * Saldo, limite e plano sao definidos so no cadastro; depois mudam apenas por {@link #debit} e {@link #refund},
 * chamados dentro da transacao do MessageService, com o cliente ja travado e o lancamento do extrato gravado junto.
 * <p>
 * Cada cliente so acessa os proprios dados: outro id vira ClientAccessDeniedException (403).
 */
@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    // Valida o documento e grava a senha como hash BCrypt. Dois cadastros simultaneos do mesmo documento passam
    // pelo existsByDocument, mas a constraint unique do banco barra o segundo (409 no ApplicationControllerAdvice)
    public Client create(Client client, String rawPassword) {
        validateDocumentType(client);
        if (clientRepository.existsByDocument(client.getDocument())) {
            throw new DocumentAlreadyExistsException(client.getDocument());
        }

        client.setPasswordHash(passwordEncoder.encode(rawPassword));
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

    // Pre-pago desconta do saldo; pos-pago desconta do limite (o campo guarda o limite restante). Sem saldo ou
    // limite: excecao (402), e a transacao do envio desfaz tudo. Quem chama precisa ter travado o cliente
    // (ClientRepository.findWithLockById) e gravar o DEBIT no extrato
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

    // Devolve o que o debit tirou: ao saldo no pre-pago, ao limite no pos-pago
    public void refund(Client client, BigDecimal amount) {
        if (client.getPlanType() == ClientPlanType.PRE_PAID) {
            client.setBalance(client.getBalance().add(amount));
        } else {
            client.setLimit(client.getLimit().add(amount));
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

    // Tamanho coerente com o tipo (CPF 11, CNPJ 14) e digitos verificadores; os dois casos viram 400
    private void validateDocumentType(Client client) {
        int expectedLength = client.getDocumentType() == ClientDocumentType.CPF ? 11 : 14;
        if (client.getDocument().length() != expectedLength) {
            throw new InvalidDocumentException(client.getDocumentType().name(), expectedLength);
        }
        if (!DocumentValidator.isValid(client.getDocumentType(), client.getDocument())) {
            throw new InvalidDocumentException(client.getDocumentType().name());
        }
    }
}
