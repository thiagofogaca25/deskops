package com.deskops.repository;

import com.deskops.domain.entity.Ativo;
import com.deskops.domain.enums.StatusAtivo;
import com.deskops.domain.enums.TipoAtivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AtivoRepository extends JpaRepository<Ativo, UUID> {

    boolean existsByTagPatrimonioIgnoreCase(String tagPatrimonio);

    @Query("""
            SELECT a FROM Ativo a
            LEFT JOIN FETCH a.colaborador c
            WHERE a.id = :id
            """)
    Optional<Ativo> findByIdWithColaborador(@Param("id") UUID id);

    @Query("""
            SELECT a FROM Ativo a
            LEFT JOIN FETCH a.colaborador c
            WHERE (:status IS NULL OR a.status = :status)
              AND (:tipo IS NULL OR a.tipo = :tipo)
              AND (:busca IS NULL OR :busca = ''
                   OR LOWER(a.tagPatrimonio) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(COALESCE(a.ipLocal, '')) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(a.modelo) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(a.fabricante) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(COALESCE(c.nome, '')) LIKE LOWER(CONCAT('%', :busca, '%')))
            ORDER BY a.tagPatrimonio ASC
            """)
    List<Ativo> buscarComFiltros(
            @Param("status") StatusAtivo status,
            @Param("tipo") TipoAtivo tipo,
            @Param("busca") String busca
    );
}
