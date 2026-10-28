package com.deskops.repository;

import com.deskops.domain.entity.Ticket;
import com.deskops.domain.enums.CategoriaTicket;
import com.deskops.domain.enums.PrioridadeTicket;
import com.deskops.domain.enums.StatusTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    long countByProtocoloStartingWith(String prefixo);

    boolean existsByProtocolo(String protocolo);

    @Query("""
            SELECT t FROM Ticket t
            JOIN FETCH t.colaborador c
            LEFT JOIN FETCH t.tecnicoAtribuido tec
            LEFT JOIN FETCH t.ativo a
            WHERE t.id = :id
            """)
    Optional<Ticket> findByIdWithAssociations(@Param("id") UUID id);

    @Query("""
            SELECT t FROM Ticket t
            JOIN FETCH t.colaborador c
            LEFT JOIN FETCH t.tecnicoAtribuido tec
            LEFT JOIN FETCH t.ativo a
            WHERE (:status IS NULL OR t.status = :status)
              AND (:prioridade IS NULL OR t.prioridade = :prioridade)
              AND (:categoria IS NULL OR t.categoria = :categoria)
              AND (:ativoId IS NULL OR a.id = :ativoId)
            ORDER BY t.dataAbertura DESC
            """)
    List<Ticket> buscarComFiltros(
            @Param("status") StatusTicket status,
            @Param("prioridade") PrioridadeTicket prioridade,
            @Param("categoria") CategoriaTicket categoria,
            @Param("ativoId") UUID ativoId
    );

    @Query("""
            SELECT t FROM Ticket t
            JOIN FETCH t.colaborador c
            LEFT JOIN FETCH t.tecnicoAtribuido tec
            LEFT JOIN FETCH t.ativo a
            WHERE a.id = :ativoId
            ORDER BY t.dataAbertura DESC
            """)
    List<Ticket> findByAtivoIdWithAssociations(@Param("ativoId") UUID ativoId);
}
