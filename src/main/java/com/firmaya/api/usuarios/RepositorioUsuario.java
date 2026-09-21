package com.firmaya.api.usuarios;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioUsuario extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByCorreoNormalizado(String correoNormalizado);

    /** CU-15, EP-13: filtros opcionales por correo, rol y estado administrativo. */
    @Query("""
            SELECT u FROM Usuario u
            WHERE (:correo IS NULL OR LOWER(u.correoElectronico) LIKE LOWER(CONCAT('%', :correo, '%')))
              AND (:rol IS NULL OR u.rolGlobal = :rol)
              AND (:estado IS NULL OR u.estadoAdministrativo = :estado)
            """)
    Page<Usuario> buscarUsuarios(@Param("correo") String correo,
                                  @Param("rol") RolGlobal rol,
                                  @Param("estado") EstadoAdministrativo estado,
                                  Pageable pageable);

    /** CU-15: proteccion del ultimo administrador activo (EP-16/EP-17). */
    long countByRolGlobalAndEstadoAdministrativo(RolGlobal rolGlobal, EstadoAdministrativo estadoAdministrativo);
}
