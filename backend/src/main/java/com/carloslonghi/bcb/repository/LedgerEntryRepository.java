package com.carloslonghi.bcb.repository;

import com.carloslonghi.bcb.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    // Mais recentes primeiro; o id desempata lancamentos no mesmo instante (debito e estorno de testes rapidos)
    @Query("select e from LedgerEntry e join fetch e.message where e.client.id = :clientId order by e.createdAt desc, e.id desc")
    List<LedgerEntry> findByClientId(Long clientId);
}
