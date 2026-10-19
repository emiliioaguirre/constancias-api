package com.universidad.constancias.repositories;

import com.universidad.constancias.models.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

// acceso a la tabla solicitud, con id numérico
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    // atender un pago a la vez para la misma solicitud
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Solicitud s where s.id = :id")
    Optional<Solicitud> buscarConBloqueo(@Param("id") Long id);
    List<Solicitud> findByEstudianteMatricula(String matricula);
}
