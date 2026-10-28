package com.deskops.service;

import com.deskops.domain.entity.Colaborador;
import com.deskops.domain.enums.PerfilUsuario;
import com.deskops.dto.ColaboradorRequest;
import com.deskops.dto.ColaboradorResponse;
import com.deskops.exception.BusinessConflictException;
import com.deskops.exception.ResourceNotFoundException;
import com.deskops.repository.ColaboradorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ColaboradorService {

    private final ColaboradorRepository colaboradorRepository;

    public ColaboradorService(ColaboradorRepository colaboradorRepository) {
        this.colaboradorRepository = colaboradorRepository;
    }

    @Transactional(readOnly = true)
    public List<ColaboradorResponse> listarTodos(PerfilUsuario perfil) {
        List<Colaborador> lista = (perfil != null)
                ? colaboradorRepository.findByPerfilOrderByNomeAsc(perfil)
                : colaboradorRepository.findAllByOrderByNomeAsc();
        return lista.stream()
                .map(ColaboradorResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ColaboradorResponse buscarPorId(UUID id) {
        return ColaboradorResponse.fromEntity(buscarEntidadePorId(id));
    }

    @Transactional(readOnly = true)
    public Colaborador buscarEntidadePorId(UUID id) {
        return colaboradorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado com o ID: " + id));
    }

    @Transactional
    public ColaboradorResponse criar(ColaboradorRequest request) {
        if (colaboradorRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessConflictException("Já existe um colaborador cadastrado com o e-mail: " + request.email());
        }

        Colaborador colaborador = new Colaborador(
                request.nome().trim(),
                request.email().trim().toLowerCase(),
                request.cargo().trim(),
                request.departamento().trim(),
                request.perfil() != null ? request.perfil() : PerfilUsuario.COLABORADOR
        );

        return ColaboradorResponse.fromEntity(colaboradorRepository.save(colaborador));
    }

    @Transactional
    public ColaboradorResponse atualizar(UUID id, ColaboradorRequest request) {
        Colaborador existente = buscarEntidadePorId(id);

        if (!existente.getEmail().equalsIgnoreCase(request.email())
                && colaboradorRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessConflictException("Já existe outro colaborador cadastrado com o e-mail: " + request.email());
        }

        existente.setNome(request.nome().trim());
        existente.setEmail(request.email().trim().toLowerCase());
        existente.setCargo(request.cargo().trim());
        existente.setDepartamento(request.departamento().trim());
        if (request.perfil() != null) {
            existente.setPerfil(request.perfil());
        }

        return ColaboradorResponse.fromEntity(colaboradorRepository.save(existente));
    }
}
