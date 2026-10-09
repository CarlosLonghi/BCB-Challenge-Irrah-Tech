package com.carloslonghi.bcb.service;

import com.carloslonghi.bcb.entity.Client;
import com.carloslonghi.bcb.entity.enums.ClientDocumentType;
import com.carloslonghi.bcb.entity.enums.ClientPlanType;
import com.carloslonghi.bcb.exception.ClientAccessDeniedException;
import com.carloslonghi.bcb.exception.CreditLimitExceededException;
import com.carloslonghi.bcb.exception.DocumentAlreadyExistsException;
import com.carloslonghi.bcb.exception.InsufficientBalanceException;
import com.carloslonghi.bcb.exception.InvalidDocumentException;
import com.carloslonghi.bcb.exception.ReferencedEntityNotFoundException;
import com.carloslonghi.bcb.repository.ClientRepository;
import com.carloslonghi.bcb.support.AuthenticatedClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;

import static com.carloslonghi.bcb.support.TestData.postPaidClient;
import static com.carloslonghi.bcb.support.TestData.prePaidClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientService clientService;

    @AfterEach
    void tearDown() {
        AuthenticatedClient.logout();
    }

    @Nested
    @DisplayName("debit - plano pré-pago")
    class DebitPrePaid {

        @Test
        @DisplayName("com saldo suficiente, desconta do saldo e salva")
        void subtractsFromBalance() {
            Client client = prePaidClient(1L, "10.00");

            clientService.debit(client, new BigDecimal("0.25"));

            assertThat(client.getBalance()).isEqualByComparingTo("9.75");
            verify(clientRepository).save(client);
        }

        @Test
        @DisplayName("com saldo exatamente igual ao custo, o saldo vai a zero")
        void balanceEqualToCostGoesToZero() {
            Client client = prePaidClient(1L, "0.50");

            clientService.debit(client, new BigDecimal("0.50"));

            assertThat(client.getBalance()).isEqualByComparingTo("0");
            verify(clientRepository).save(client);
        }

        @Test
        @DisplayName("compara valores pelo número, não pela escala (0.5 == 0.50)")
        void comparesIgnoringScale() {
            Client client = prePaidClient(1L, "0.5");

            clientService.debit(client, new BigDecimal("0.50"));

            assertThat(client.getBalance()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("com saldo insuficiente, lança exceção sem alterar o saldo nem salvar")
        void insufficientBalance() {
            Client client = prePaidClient(1L, "0.24");

            assertThatThrownBy(() -> clientService.debit(client, new BigDecimal("0.25")))
                    .isInstanceOf(InsufficientBalanceException.class)
                    .hasMessage("Saldo insuficiente.");

            assertThat(client.getBalance()).isEqualByComparingTo("0.24");
            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("com saldo zerado, recusa o débito")
        void zeroBalance() {
            Client client = prePaidClient(1L, "0.00");

            assertThatThrownBy(() -> clientService.debit(client, new BigDecimal("0.25")))
                    .isInstanceOf(InsufficientBalanceException.class);
            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("não usa nem altera o limite")
        void doesNotTouchLimit() {
            Client client = prePaidClient(1L, "0.10");
            client.setLimit(new BigDecimal("100.00"));

            assertThatThrownBy(() -> clientService.debit(client, new BigDecimal("0.25")))
                    .isInstanceOf(InsufficientBalanceException.class);
            assertThat(client.getLimit()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("débitos seguidos acumulam no saldo")
        void consecutiveDebits() {
            Client client = prePaidClient(1L, "1.00");

            clientService.debit(client, new BigDecimal("0.50"));
            clientService.debit(client, new BigDecimal("0.25"));
            clientService.debit(client, new BigDecimal("0.25"));

            assertThat(client.getBalance()).isEqualByComparingTo("0");
            assertThatThrownBy(() -> clientService.debit(client, new BigDecimal("0.25")))
                    .isInstanceOf(InsufficientBalanceException.class);
        }

        @Test
        @DisplayName("um centavo a menos que o custo já é recusado")
        void oneCentShort() {
            Client client = prePaidClient(1L, "0.49");

            assertThatThrownBy(() -> clientService.debit(client, new BigDecimal("0.50")))
                    .isInstanceOf(InsufficientBalanceException.class);
            assertThat(client.getBalance()).isEqualByComparingTo("0.49");
        }

        @Test
        @DisplayName("se o banco falhar ao salvar, a exceção propaga (a transação do envio faz rollback)")
        void saveFailurePropagates() {
            Client client = prePaidClient(1L, "10.00");
            when(clientRepository.save(client)).thenThrow(new DataAccessResourceFailureException("banco fora do ar"));

            assertThatThrownBy(() -> clientService.debit(client, new BigDecimal("0.25")))
                    .isInstanceOf(DataAccessResourceFailureException.class);
        }
    }

    @Nested
    @DisplayName("debit - plano pós-pago")
    class DebitPostPaid {

        @Test
        @DisplayName("dentro do limite, desconta do limite e salva")
        void subtractsFromLimit() {
            Client client = postPaidClient(1L, "5.00");

            clientService.debit(client, new BigDecimal("0.50"));

            assertThat(client.getLimit()).isEqualByComparingTo("4.50");
            verify(clientRepository).save(client);
        }

        @Test
        @DisplayName("consumindo exatamente o limite, ele vai a zero")
        void limitEqualToCostGoesToZero() {
            Client client = postPaidClient(1L, "0.25");

            clientService.debit(client, new BigDecimal("0.25"));

            assertThat(client.getLimit()).isEqualByComparingTo("0");
            verify(clientRepository).save(client);
        }

        @Test
        @DisplayName("acima do limite, lança exceção sem alterar o limite nem salvar")
        void limitExceeded() {
            Client client = postPaidClient(1L, "0.49");

            assertThatThrownBy(() -> clientService.debit(client, new BigDecimal("0.50")))
                    .isInstanceOf(CreditLimitExceededException.class)
                    .hasMessage("Limite de consumo excedido.");

            assertThat(client.getLimit()).isEqualByComparingTo("0.49");
            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("não usa nem altera o saldo")
        void doesNotTouchBalance() {
            Client client = postPaidClient(1L, "1.00");
            client.setBalance(new BigDecimal("50.00"));

            clientService.debit(client, new BigDecimal("0.25"));

            assertThat(client.getBalance()).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("com limite zerado, recusa o débito")
        void zeroLimit() {
            Client client = postPaidClient(1L, "0.00");

            assertThatThrownBy(() -> clientService.debit(client, new BigDecimal("0.25")))
                    .isInstanceOf(CreditLimitExceededException.class);
            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("saldo alto não salva o pós-pago sem limite")
        void balanceDoesNotCoverPostPaid() {
            Client client = postPaidClient(1L, "0.10");
            client.setBalance(new BigDecimal("100.00"));

            assertThatThrownBy(() -> clientService.debit(client, new BigDecimal("0.25")))
                    .isInstanceOf(CreditLimitExceededException.class);
            assertThat(client.getBalance()).isEqualByComparingTo("100.00");
            assertThat(client.getLimit()).isEqualByComparingTo("0.10");
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("salva CPF com 11 dígitos")
        void savesCpf() {
            Client client = prePaidClient(null, "10.00");
            when(clientRepository.existsByDocument("12345678901")).thenReturn(false);
            when(clientRepository.save(client)).thenReturn(client);

            assertThat(clientService.create(client)).isSameAs(client);
        }

        @Test
        @DisplayName("salva CNPJ com 14 dígitos")
        void savesCnpj() {
            Client client = prePaidClient(null, "10.00");
            client.setDocumentType(ClientDocumentType.CNPJ);
            client.setDocument("12345678000199");
            when(clientRepository.save(client)).thenReturn(client);

            assertThat(clientService.create(client)).isSameAs(client);
        }

        @Test
        @DisplayName("recusa documento que não combina com o tipo")
        void invalidDocumentLength() {
            Client client = prePaidClient(null, "10.00");
            client.setDocument("12345678000199");

            assertThatThrownBy(() -> clientService.create(client))
                    .isInstanceOf(InvalidDocumentException.class)
                    .hasMessage("CPF deve ter 11 dígitos.");
            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("recusa documento já cadastrado")
        void duplicatedDocument() {
            Client client = prePaidClient(null, "10.00");
            when(clientRepository.existsByDocument("12345678901")).thenReturn(true);

            assertThatThrownBy(() -> clientService.create(client))
                    .isInstanceOf(DocumentAlreadyExistsException.class);
            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("recusa CNPJ com 11 dígitos")
        void cnpjWithCpfLength() {
            Client client = prePaidClient(null, "10.00");
            client.setDocumentType(ClientDocumentType.CNPJ);

            assertThatThrownBy(() -> clientService.create(client))
                    .isInstanceOf(InvalidDocumentException.class)
                    .hasMessage("CNPJ deve ter 14 dígitos.");
            verify(clientRepository, never()).existsByDocument(any());
            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("cadastro simultâneo do mesmo documento: a violação da constraint unique propaga")
        void concurrentDuplicate() {
            Client client = prePaidClient(null, "10.00");
            when(clientRepository.save(client)).thenThrow(new DataIntegrityViolationException("uk_clients_document"));

            assertThatThrownBy(() -> clientService.create(client))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("consulta e alteração")
    class FindAndUpdate {

        @Test
        @DisplayName("findById devolve o próprio cliente")
        void findOwnClient() {
            AuthenticatedClient.login(1L);
            Client client = prePaidClient(1L, "10.00");
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

            assertThat(clientService.findById(1L)).isSameAs(client);
        }

        @Test
        @DisplayName("findById de outro cliente é negado sem consultar o banco")
        void findOtherClientDenied() {
            AuthenticatedClient.login(1L);

            assertThatThrownBy(() -> clientService.findById(2L))
                    .isInstanceOf(ClientAccessDeniedException.class);
            verify(clientRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("findById de cliente inexistente lança not found")
        void findMissingClient() {
            AuthenticatedClient.login(1L);
            when(clientRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.findById(1L))
                    .isInstanceOf(ReferencedEntityNotFoundException.class);
        }

        @Test
        @DisplayName("findAuthenticated devolve só o cliente autenticado")
        void findAuthenticated() {
            AuthenticatedClient.login(1L);
            Client client = prePaidClient(1L, "10.00");
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

            assertThat(clientService.findAuthenticated()).containsExactly(client);
        }

        @Test
        @DisplayName("findAuthenticated devolve lista vazia se o cliente não existe mais")
        void findAuthenticatedMissing() {
            AuthenticatedClient.login(1L);
            when(clientRepository.findById(1L)).thenReturn(Optional.empty());

            assertThat(clientService.findAuthenticated()).isEmpty();
        }

        @Test
        @DisplayName("updateName altera só o nome; saldo, limite e plano ficam iguais")
        void updateNameOnly() {
            AuthenticatedClient.login(1L);
            Client client = prePaidClient(1L, "10.00");
            client.setLimit(new BigDecimal("3.00"));
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(clientRepository.save(client)).thenReturn(client);

            Client updated = clientService.updateName(1L, "Novo Nome");

            assertThat(updated.getName()).isEqualTo("Novo Nome");
            assertThat(updated.getBalance()).isEqualByComparingTo("10.00");
            assertThat(updated.getLimit()).isEqualByComparingTo("3.00");
            assertThat(updated.getPlanType()).isEqualTo(ClientPlanType.PRE_PAID);
        }

        @Test
        @DisplayName("updateName de outro cliente é negado")
        void updateOtherClientDenied() {
            AuthenticatedClient.login(1L);

            assertThatThrownBy(() -> clientService.updateName(2L, "Outro"))
                    .isInstanceOf(ClientAccessDeniedException.class);
            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("updateName de cliente que não existe mais lança not found sem salvar")
        void updateMissingClient() {
            AuthenticatedClient.login(1L);
            when(clientRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.updateName(1L, "Novo Nome"))
                    .isInstanceOf(ReferencedEntityNotFoundException.class);
            verify(clientRepository, never()).save(any());
        }
    }
}
