package com.deskops.repository;

import com.deskops.domain.entity.TicketNota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketNotaRepository extends JpaRepository<TicketNota, UUID> {

    @Query("""
            SELECT n FROM TicketNota n
            JOIN FETCH n.autor a
            WHERE n.ticket.id = :ticketId
              AND (:incluirPrivadas = true OR n.privadaTi = false)
            ORDER BY n.dataRegistro ASC
            """)
    List<TicketNota> findByTicketIdWithAutor(
            @Param("ticketId") UUID ticketId,
            @Param("incluirPrivadas") boolean incluirPrivadas
    );
}
