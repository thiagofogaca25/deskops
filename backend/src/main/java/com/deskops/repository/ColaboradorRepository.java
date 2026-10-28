package com.deskops.repository;

import com.deskops.domain.entity.Colaborador;
import com.deskops.domain.enums.PerfilUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ColaboradorRepository extends JpaRepository<Colaborador, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<Colaborador> findByEmailIgnoreCase(String email);

    List<Colaborador> findByPerfilOrderByNomeAsc(PerfilUsuario perfil);

    List<Colaborador> findAllByOrderByNomeAsc();
}
