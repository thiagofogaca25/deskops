package com.deskops.repository;

import com.deskops.domain.entity.HistoricoAtivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HistoricoAtivoRepository extends JpaRepository<HistoricoAtivo, UUID> {

    List<HistoricoAtivo> findByAtivoIdOrderByDataRegistroDesc(UUID ativoId);
}
