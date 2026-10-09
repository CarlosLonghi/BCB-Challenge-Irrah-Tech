package com.carloslonghi.bcb.integration;

import com.carloslonghi.bcb.entity.Client;
import com.carloslonghi.bcb.repository.ClientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static com.carloslonghi.bcb.support.TestData.prePaidClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// O contexto so sobe se o Flyway aplicar as migrations e o ddl-auto=validate aceitar o schema
class SchemaIT extends IntegrationTest {

    @Autowired
    private ClientRepository clientRepository;

    @Test
    @DisplayName("o Flyway aplicou a V1 com sucesso")
    void flywayApplied() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success", Integer.class))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("a constraint unique barra documento duplicado mesmo sem passar pelo service")
    void uniqueDocument() {
        clientRepository.saveAndFlush(prePaidClient(null, "10.00"));
        Client duplicated = prePaidClient(null, "5.00");

        assertThatThrownBy(() -> clientRepository.saveAndFlush(duplicated))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("o CHECK barra valor de enum desconhecido gravado direto no banco")
    void enumCheckConstraint() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO tb_clients (name, document, document_type, plan_type, balance, client_limit, active, created_at, updated_at)
                VALUES ('X', '12345678901', 'RG', 'PRE_PAID', 0, 0, true, now(), now())
                """))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
