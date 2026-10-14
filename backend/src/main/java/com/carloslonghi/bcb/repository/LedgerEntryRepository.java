package com.carloslonghi.bcb.repository;

import com.carloslonghi.bcb.entity.LedgerEntry;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    // Paginas por cursor (id), mais recentes primeiro; o mapper so le o id da mensagem, sem carrega-la
    List<LedgerEntry> findByClientIdOrderByIdDesc(Long clientId, Limit limit);
    List<LedgerEntry> findByClientIdAndIdLessThanOrderByIdDesc(Long clientId, Long before, Limit limit);
}
