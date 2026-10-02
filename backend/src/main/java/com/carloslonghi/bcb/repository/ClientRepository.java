package com.carloslonghi.bcb.repository;

import com.carloslonghi.bcb.entity.Client;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByDocument(String document);
    boolean existsByDocument(String document);

    // Dois envios simultaneos do mesmo cliente esperam um pelo outro, assim os dois nao leem o mesmo saldo antes do debito
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Client> findWithLockById(Long id);
}
